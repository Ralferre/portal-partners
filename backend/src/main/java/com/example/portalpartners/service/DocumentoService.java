package com.example.portalpartners.service;

import com.example.portalpartners.documento.DocumentoSpecification;
import com.example.portalpartners.dto.CreateDocumentoRequest;
import com.example.portalpartners.dto.DocumentoResponse;
import com.example.portalpartners.dto.TypeReferenceFile;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.Funcionario;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoService {
    private final DocumentoRepository documentoRepository;
    private final ContratadaRepository contratadaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final MinioService minioService;

    @Transactional
    public DocumentoResponse uploadDocumento(CreateDocumentoRequest dto) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
            throw new BusinessRulesException("Usuário sem permissão");
        }

        validarArquivo(dto.arquivo());

        Documento documento = new Documento();
        documento.setTipoDocumento(dto.tipoDocumento());
        documento.setStatusDocumento(StatusDocumento.PENDENTE);
        documento.setDataPostagem(LocalDateTime.now());

        if (dto.tipoReferenciaDocumento() == TypeReferenceFile.CONTRATADA) {

            Contratada contratada;
            if (dto.contratadaId() != null) {
                contratada = contratadaRepository.findById(dto.contratadaId())
                        .orElseThrow(() -> new BusinessRulesException("Contratada não encontrada"));
            } else {
                contratada = usuarioLogadoService.getContratadaLogada();
            }

            documento.setContratada(contratada);
            documento.setFuncionario(null);
        } else if (dto.tipoReferenciaDocumento() == TypeReferenceFile.FUNCIONARIO) {
            if (dto.funcionarioId() == null) {
                throw new BusinessRulesException("Funcionário deve ser informado");
            }

            Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                    .orElseThrow(() -> new BusinessRulesException("Funcionário não encontrado"));

            documento.setFuncionario(funcionario);
            documento.setContratada(funcionario.getContratada());
        } else {
            throw new BusinessRulesException("Tipo de referência inválido");
        }

        documento.setNomeArquivo(dto.arquivo().getOriginalFilename());
        documento.setContentType(dto.arquivo().getContentType());

        String objectName = minioService.uploadFile(
                dto.arquivo(),
                documento.getContratada() != null ? documento.getContratada().getNome() : "contratada",
                documento.getFuncionario() != null ? documento.getFuncionario().getNomeCompleto() : null,
                dto.tipoDocumento()
        );
        documento.setArquivoPath(objectName);

        Documento saved = documentoRepository.save(documento);
        return convertToResponse(saved);
    }

    @Transactional
    public DocumentoResponse updateStatus(Long documentoId, StatusDocumento novoStatus) {

        Usuario usuario = usuarioLogadoService.getUsuario();

        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado"));

        // 🔐 valida permissão por perfil
        validarPermissaoAlteracao(usuario, documento);

        // 🎯 regra de transição (opcional mas recomendado)
        validarMudancaStatus(novoStatus);

        documento.setStatusDocumento(novoStatus);
        documento.setDataStatusAtualizado(LocalDateTime.now());

        Documento salvo = documentoRepository.save(documento);

        return convertToResponse(salvo);
    }

    @Transactional
    public DownloadPayload download(Long documentoId) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado"));

        // reutiliza regras de acesso (quem pode ver / mexer neste documento)
        // CONTRATADA pode baixar apenas dela; CONTRATANTE apenas suas contratadas; ADMIN tudo.
        if (usuario.getRole() == Role.ADMIN) {
            // ok
        } else if (usuario.getRole() == Role.CONTRATADA) {
            Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();
            if (documento.getContratada() == null
                    || !documento.getContratada().getId().equals(contratadaLogada.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
        } else if (usuario.getRole() == Role.CONTRATANTE) {
            Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
            if (documento.getContratada() == null
                    || documento.getContratada().getContratante() == null
                    || !documento.getContratada().getContratante().getId().equals(contratanteLogado.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
        } else {
            throw new ForbiddenException("Perfil sem permissão");
        }

        // marca como baixado pela contratante (para badge de "novos")
        if (usuario.getRole() == Role.CONTRATANTE && documento.getDataDownloadContratante() == null) {
            documento.setDataDownloadContratante(LocalDateTime.now());
            documentoRepository.save(documento);
        }

        InputStream stream = minioService.getObjectStream(documento.getArquivoPath());
        Resource resource = new InputStreamResource(stream);

        String contentType = documento.getContentType();
        if (contentType == null || contentType.isBlank()) {
            try {
                contentType = minioService.statObject(documento.getArquivoPath()).contentType();
            } catch (Exception ignored) {
                contentType = "application/octet-stream";
            }
        }

        return new DownloadPayload(resource, documento.getNomeArquivo(), contentType);
    }

    public long countNovosDocumentosParaContratante() {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() != Role.CONTRATANTE) {
            throw new AccessDeniedException("Apenas CONTRATANTE pode consultar novos documentos");
        }

        Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
        Specification<Documento> spec = Specification.<Documento>where(null)
                .and(DocumentoSpecification.porContratanteId(contratanteLogado.getId()))
                .and((Specification<Documento>)
                        (root, query, cb) -> cb.isNull(root.get("dataDownloadContratante")));

        return documentoRepository.count(spec);
    }

    public List<TipoDocumento> listarTiposDocumento() {
        return List.of(TipoDocumento.values());
    }

    @Transactional
    public void deletarDocumento(Long id) {

        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento não encontrado"));

        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN) {
            documentoRepository.delete(documento);
            return;
        }

        if (usuario.getRole() == Role.CONTRATADA) {
            Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();

            if (documento.getContratada() == null
                    || !documento.getContratada().getId().equals(contratadaLogada.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }

            documentoRepository.delete(documento);
            return;
        }

        if (usuario.getRole() == Role.CONTRATANTE) {
            Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();

            if (documento.getContratada() == null
                    || documento.getContratada().getContratante() == null
                    || !documento.getContratada().getContratante().getId().equals(contratanteLogado.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }

            documentoRepository.delete(documento);
            return;
        }

        throw new ForbiddenException("Perfil sem permissão");
    }

    public List<Documento> findByContratadaNome(String contratadaNome) {
        return documentoRepository.findByContratadaNome(contratadaNome);
    }

    public List<Documento> findByFuncionarioId(Long funcionarioId) {
        return documentoRepository.findByFuncionarioId(funcionarioId);
    }

    public List<Documento> findByContratadaNomeAndTipo(String contratadaNome, TipoDocumento tipoDocumento) {
        return documentoRepository.findByContratadaNomeAndTipoDocumento(contratadaNome, tipoDocumento);
    }

    public Page<Documento> findByFuncionarioNomeContainingIgnoreCase(String nome, Pageable pageable) {
        return documentoRepository.findByFuncionarioNomeCompletoContainingIgnoreCase(nome, pageable);
    }

    public Page<Documento> findAll(int page, int size) {
        return documentoRepository.findAll(PageRequest.of(page, size));
    }

    private Specification<Documento> restricaoPorPerfil(Usuario usuario) {

        if (usuario.getRole() == Role.ADMIN) {
            return Specification.where(null);
        }

        if (usuario.getRole() == Role.CONTRATADA) {
            Contratada contratada = usuarioLogadoService.getContratadaLogada();
            return DocumentoSpecification.porContratadaId(contratada.getId());
        }

        if (usuario.getRole() == Role.CONTRATANTE) {
            Contratante contratante = usuarioLogadoService.getContratanteLogada();
            return DocumentoSpecification.porContratanteId(contratante.getId());
        }

        throw new AccessDeniedException("Perfil sem permissão");
    }

    private void validarPermissaoAlteracao(Usuario usuario, Documento documento) {

        // 👑 ADMIN pode tudo
        if (usuario.getRole() == Role.ADMIN) {
            return;
        }

        // 🏢 CONTRATANTE — só suas contratadas
        if (usuario.getRole() == Role.CONTRATANTE) {

            Contratante contratanteLogado =
                    usuarioLogadoService.getContratanteLogada();

            Long contratanteDoDocumento =
                    documento.getContratada()
                            .getContratante()
                            .getId();

            if (!contratanteLogado.getId().equals(contratanteDoDocumento)) {
                throw new AccessDeniedException(
                        "Você não pode alterar documentos de outra contratante");
            }

            return;
        }

        // 🚫 qualquer outro perfil
        throw new AccessDeniedException("Perfil sem permissão");
    }

    private void validarMudancaStatus(StatusDocumento novoStatus) {

        if (novoStatus == null) {
            throw new BusinessRulesException("Status deve ser informado");
        }

        if (novoStatus != StatusDocumento.PENDENTE
                && novoStatus != StatusDocumento.APROVADO
                && novoStatus != StatusDocumento.REPROVADO) {
            throw new BusinessRulesException("Status inválido. Use PENDENTE, APROVADO ou REPROVADO");
        }
    }

    public record DownloadPayload(Resource resource, String filename, String contentType) {}

    public Page<DocumentoResponse> filtrar(
            String contratadaNome,
            String funcionarioNome,
            TipoDocumento tipo,
            StatusDocumento status,
            Pageable pageable) {

        Usuario usuario = usuarioLogadoService.getUsuario();

        Specification<Documento> spec = Specification.where(null);

        // 🔐 filtro por perfil
        spec = spec.and(restricaoPorPerfil(usuario));

        // 🔎 filtros opcionais
        if (contratadaNome != null && !contratadaNome.isBlank()) {
            spec = spec.and(DocumentoSpecification.contratadaNomeLike(contratadaNome));
        }

        if (funcionarioNome != null && !funcionarioNome.isBlank()) {
            spec = spec.and(DocumentoSpecification.funcionarioNomeLike(funcionarioNome));
        }

        if (tipo != null) {
            spec = spec.and(DocumentoSpecification.tipoEquals(tipo));
        }

        if (status != null) {
            spec = spec.and(DocumentoSpecification.statusEquals(status));
        }

        Page<Documento> page = documentoRepository.findAll(spec, pageable);

        return page.map(this::convertToResponse);
    }
    
    private DocumentoResponse convertToResponse(Documento documento) {
        DocumentoResponse dto = new DocumentoResponse();
        dto.setId(documento.getId());
        dto.setTipoDocumento(documento.getTipoDocumento());
        dto.setNomeArquivo(documento.getNomeArquivo());
        dto.setArquivoPath(documento.getArquivoPath());
        dto.setContentType(documento.getContentType());
        dto.setStatusDocumento(documento.getStatusDocumento());
        dto.setDataPostagem(documento.getDataPostagem());
        dto.setDataDownloadContratante(documento.getDataDownloadContratante());
        dto.setDataStatusAtualizado(documento.getDataStatusAtualizado());
        dto.setContratadaNome(
                documento.getContratada() != null ? documento.getContratada().getNome() : null
        );
        dto.setFuncionarioNome(
                documento.getFuncionario() != null ? documento.getFuncionario().getNomeCompleto() : null
        );
        return dto;
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessRulesException("Arquivo é obrigatório");
        }

        long maxSize = 10_000_000; // 10MB
        if (arquivo.getSize() > maxSize) {
            throw new BusinessRulesException("Arquivo não pode ser maior que 10MB");
        }

        String[] tiposPermitidos = {
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
        };

        String contentType = arquivo.getContentType();
        if (contentType == null || !Arrays.asList(tiposPermitidos).contains(contentType)) {
            throw new BusinessRulesException(
                "Tipo de arquivo não permitido. Apenas PDF, JPEG, PNG e DOCX são aceitos"
            );
        }
    }

}
