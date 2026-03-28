package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.documento.DocumentoSpecification;
import com.example.portalpartners.dto.*;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.*;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final ContratadaRepository contratadaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final MinioService minioService;
    private final LgpdService lgpdService;

    // =========================================================================
    // ARQUITETURA ZERO-COPY — Presigned URLs
    // Os bytes dos documentos NUNCA passam pelo processo Java.
    // =========================================================================

    /**
     * Etapa 1 do upload Zero-Copy: valida permissoes e LGPD, gera objectKey
     * opaco (UUID) e retorna presigned PUT URL para upload direto ao MinIO.
     */
    @Auditavel(acao = "SOLICITAR_UPLOAD", entidade = "Documento")
    @Transactional
    public SolicitarUploadResponse solicitarUpload(SolicitarUploadRequest request) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
            throw new BusinessRulesException("Apenas CONTRATADA pode fazer upload de documentos");
        }

        if (lgpdService.exigeConsentimentoParaDocumentosPessoais(request.tipoReferencia())) {
            lgpdService.exigirConsentimentoValido();
        }

        validarContentType(request.contentType());
        validarTamanho(request.tamanhoBytes());

        Documento documento = new Documento();
        documento.setTipoDocumento(request.tipoDocumento());
        documento.setStatusDocumento(StatusDocumento.PENDENTE);
        documento.setDataPostagem(LocalDateTime.now());
        documento.setContentType(request.contentType());
        documento.setTamanhoBytes(request.tamanhoBytes());

        // Nome original cifrado com AES-256-GCM via @Convert
        documento.setNomeArquivoOriginal(request.nomeArquivo());
        // Campo legado preenchido com valor neutro (sem informacao pessoal)
        documento.setNomeArquivo("documento-" + LocalDateTime.now().toLocalDate());

        // ObjectKey: UUID v4 opaco — sem qualquer relacao com o nome real
        String objectKey = UUID.randomUUID().toString();
        documento.setObjectKey(objectKey);

        resolverReferencia(documento, request, usuario);

        Documento saved = documentoRepository.save(documento);
        String uploadUrl = minioService.gerarPresignedPutUrl(objectKey);

        return new SolicitarUploadResponse(saved.getId(), objectKey, uploadUrl, 600);
    }

    /**
     * Etapa 2 do upload Zero-Copy: frontend informa conclusao do PUT direto
     * ao MinIO. Backend atualiza status e finaliza o registro.
     */
    @Auditavel(acao = "UPLOAD_CONCLUIDO", entidade = "Documento")
    @Transactional
    public DocumentoResponse confirmarUpload(Long documentoId) {
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado"));

        validarPermissaoContratada(documento);
        documento.setStatusDocumento(StatusDocumento.PENDENTE);
        return convertToResponse(documentoRepository.save(documento));
    }

    /**
     * Download Zero-Copy: valida permissoes, gera presigned GET URL e retorna
     * ao frontend. Os bytes do arquivo NUNCA passam pelo backend.
     * Registra evento de auditoria de forma assincrona.
     */
    @Auditavel(acao = "DOWNLOAD_SOLICITADO", entidade = "Documento")
    @Transactional
    public SolicitarDownloadResponse solicitarDownload(Long documentoId) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado"));

        validarPermissaoDownload(usuario, documento);

        // Marca download pela contratante (para badge de "novos")
        if (usuario.getRole() == Role.CONTRATANTE
                && documento.getDataDownloadContratante() == null) {
            documento.setDataDownloadContratante(LocalDateTime.now());
            documentoRepository.save(documento);
        }

        // Usa objectKey (novo) ou arquivoPath (legado)
        String key = documento.getObjectKey() != null
                ? documento.getObjectKey()
                : documento.getArquivoPath();

        String downloadUrl = minioService.gerarPresignedGetUrl(key);

        // Nome exibido: usa nomeArquivoOriginal (decifrado pelo converter) se disponivel
        String nomeExibido = documento.getNomeArquivoOriginal() != null
                ? documento.getNomeArquivoOriginal()
                : documento.getNomeArquivo();

        return new SolicitarDownloadResponse(downloadUrl, nomeExibido,
                documento.getContentType(), 900);
    }

    // =========================================================================
    // UPLOAD LEGADO (retrocompatibilidade — bytes passam pelo backend)
    // Mantido para nao quebrar o frontend atual. Migrar para Zero-Copy gradualmente.
    // =========================================================================

    @Auditavel(acao = "UPLOAD_LEGADO", entidade = "Documento")
    @Transactional
    public DocumentoResponse uploadDocumento(CreateDocumentoRequest dto) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
            throw new BusinessRulesException("Usuário sem permissão");
        }

        if (lgpdService.exigeConsentimentoParaDocumentosPessoais(dto.tipoReferenciaDocumento())) {
            lgpdService.exigirConsentimentoValido();
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
                        .orElseThrow(() -> new BusinessRulesException("Contratada nao encontrada"));
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

    // =========================================================================
    // DOWNLOAD LEGADO (retrocompatibilidade — bytes passam pelo backend)
    // =========================================================================

    @Auditavel(acao = "DOWNLOAD_LEGADO", entidade = "Documento")
    @Transactional
    public DownloadPayload download(Long documentoId) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado"));

        validarPermissaoDownload(usuario, documento);

        if (usuario.getRole() == Role.CONTRATANTE
                && documento.getDataDownloadContratante() == null) {
            documento.setDataDownloadContratante(LocalDateTime.now());
            documentoRepository.save(documento);
        }

        String key = documento.getObjectKey() != null
                ? documento.getObjectKey()
                : documento.getArquivoPath();

        InputStream stream   = minioService.getObjectStream(key);
        Resource resource    = new InputStreamResource(stream);
        String contentType   = documento.getContentType();

        if (contentType == null || contentType.isBlank()) {
            try {
                contentType = minioService.statObject(key).contentType();
            } catch (Exception ignored) {
                contentType = "application/octet-stream";
            }
        }

        String nomeExibido = documento.getNomeArquivoOriginal() != null
                ? documento.getNomeArquivoOriginal()
                : documento.getNomeArquivo();

        return new DownloadPayload(resource, nomeExibido, contentType);
    }

    // =========================================================================
    // STATUS, FILTROS, EXCLUSAO
    // =========================================================================

    @Auditavel(acao = "STATUS_DOCUMENTO_ALTERADO", entidade = "Documento")
    @Transactional
    public DocumentoResponse updateStatus(Long documentoId, StatusDocumento novoStatus) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        Documento documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado"));

        validarPermissaoAlteracao(usuario, documento);
        validarMudancaStatus(novoStatus);

        documento.setStatusDocumento(novoStatus);
        documento.setDataStatusAtualizado(LocalDateTime.now());

        return convertToResponse(documentoRepository.save(documento));
    }

    @Auditavel(acao = "DOCUMENTO_EXCLUIDO", entidade = "Documento")
    @Transactional
    public void deletarDocumento(Long id) {
        Documento documento = documentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado"));

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
                    || !documento.getContratada().getContratante().getId()
                          .equals(contratanteLogado.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
            documentoRepository.delete(documento);
            return;
        }

        throw new ForbiddenException("Perfil sem permissão");
    }

    public long countNovosDocumentosParaContratante() {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() != Role.CONTRATANTE) {
            throw new AccessDeniedException("Apenas CONTRATANTE pode consultar novos documentos");
        }
        Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
        Specification<Documento> spec = Specification.<Documento>where(null)
                .and(DocumentoSpecification.porContratanteId(contratanteLogado.getId()))
                .and((root, query, cb) -> cb.isNull(root.get("dataDownloadContratante")));
        return documentoRepository.count(spec);
    }

    public List<TipoDocumento> listarTiposDocumento() {
        return List.of(TipoDocumento.values());
    }

    public List<Documento> findByContratadaNome(String contratadaNome) {
        return documentoRepository.findByContratadaNome(contratadaNome);
    }

    public List<Documento> findByFuncionarioId(Long funcionarioId) {
        return documentoRepository.findByFuncionarioId(funcionarioId);
    }

    public List<Documento> findByContratadaNomeAndTipo(String contratadaNome, TipoDocumento tipo) {
        return documentoRepository.findByContratadaNomeAndTipoDocumento(contratadaNome, tipo);
    }

    public Page<Documento> findByFuncionarioNomeContainingIgnoreCase(String nome, Pageable pageable) {
        return documentoRepository.findByFuncionarioNomeCompletoContainingIgnoreCase(nome, pageable);
    }

    public Page<Documento> findAll(int page, int size) {
        return documentoRepository.findAll(PageRequest.of(page, size));
    }

    public Page<DocumentoResponse> filtrar(
            String contratadaNome,
            String funcionarioNome,
            TipoDocumento tipo,
            StatusDocumento status,
            Pageable pageable) {

        Usuario usuario = usuarioLogadoService.getUsuario();
        Specification<Documento> spec = Specification.where(null);
        spec = spec.and(restricaoPorPerfil(usuario));

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

        return documentoRepository.findAll(spec, pageable).map(this::convertToResponse);
    }

    // =========================================================================
    // Auxiliares privados
    // =========================================================================

    private void resolverReferencia(Documento documento,
                                    SolicitarUploadRequest request,
                                    Usuario usuario) {
        if (request.tipoReferencia() == TypeReferenceFile.CONTRATADA) {
            Contratada contratada;
            if (request.contratadaId() != null) {
                contratada = contratadaRepository.findById(request.contratadaId())
                        .orElseThrow(() -> new BusinessRulesException("Contratada nao encontrada"));
            } else {
                contratada = usuarioLogadoService.getContratadaLogada();
            }
            documento.setContratada(contratada);
        } else if (request.tipoReferencia() == TypeReferenceFile.FUNCIONARIO) {
            if (request.funcionarioId() == null) {
                throw new BusinessRulesException("FuncionarioId deve ser informado");
            }
            Funcionario funcionario = funcionarioRepository.findById(request.funcionarioId())
                    .orElseThrow(() -> new BusinessRulesException("Funcionario nao encontrado"));
            documento.setFuncionario(funcionario);
            documento.setContratada(funcionario.getContratada());
        } else {
            throw new BusinessRulesException("Tipo de referencia invalido");
        }
    }

    private void validarPermissaoContratada(Documento documento) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN) return;
        if (usuario.getRole() == Role.CONTRATADA) {
            Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();
            if (documento.getContratada() == null
                    || !documento.getContratada().getId().equals(contratadaLogada.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
        }
    }

    private void validarPermissaoDownload(Usuario usuario, Documento documento) {
        if (usuario.getRole() == Role.ADMIN) return;
        if (usuario.getRole() == Role.CONTRATADA) {
            Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();
            if (documento.getContratada() == null
                    || !documento.getContratada().getId().equals(contratadaLogada.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
            return;
        }
        if (usuario.getRole() == Role.CONTRATANTE) {
            Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
            if (documento.getContratada() == null
                    || documento.getContratada().getContratante() == null
                    || !documento.getContratada().getContratante().getId()
                          .equals(contratanteLogado.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
            return;
        }
        throw new ForbiddenException("Perfil sem permissão");
    }

    private Specification<Documento> restricaoPorPerfil(Usuario usuario) {
        if (usuario.getRole() == Role.ADMIN) return Specification.where(null);
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
        if (usuario.getRole() == Role.ADMIN) return;
        if (usuario.getRole() == Role.CONTRATANTE) {
            Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
            if (!contratanteLogado.getId().equals(
                    documento.getContratada().getContratante().getId())) {
                throw new AccessDeniedException("Você não pode alterar documentos de outra contratante");
            }
            return;
        }
        throw new AccessDeniedException("Perfil sem permissão");
    }

    private void validarMudancaStatus(StatusDocumento novoStatus) {
        if (novoStatus == null) throw new BusinessRulesException("Status deve ser informado");
        if (novoStatus != StatusDocumento.PENDENTE
                && novoStatus != StatusDocumento.APROVADO
                && novoStatus != StatusDocumento.REPROVADO) {
            throw new BusinessRulesException("Status inválido. Use PENDENTE, APROVADO ou REPROVADO");
        }
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BusinessRulesException("Arquivo é obrigatório");
        }
        if (arquivo.getSize() > 10_000_000L) {
            throw new BusinessRulesException("Arquivo não pode ser maior que 10MB");
        }
        String[] tiposPermitidos = {
            "application/pdf", "image/jpeg", "image/jpg", "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
        };
        String contentType = arquivo.getContentType();
        if (contentType == null || !Arrays.asList(tiposPermitidos).contains(contentType)) {
            throw new BusinessRulesException(
                    "Tipo de arquivo não permitido. Apenas PDF, JPEG, PNG e DOCX são aceitos");
        }
    }

    private void validarContentType(String contentType) {
        String[] tiposPermitidos = {
            "application/pdf", "image/jpeg", "image/jpg", "image/png",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
        };
        if (contentType == null || !Arrays.asList(tiposPermitidos).contains(contentType)) {
            throw new BusinessRulesException(
                    "Tipo de arquivo não permitido. Apenas PDF, JPEG, PNG e DOCX são aceitos");
        }
    }

    private void validarTamanho(Long tamanhoBytes) {
        if (tamanhoBytes == null || tamanhoBytes <= 0) {
            throw new BusinessRulesException("Tamanho do arquivo invalido");
        }
        if (tamanhoBytes > 10_000_000L) {
            throw new BusinessRulesException("Arquivo não pode ser maior que 10MB");
        }
    }

    private DocumentoResponse convertToResponse(Documento documento) {
        DocumentoResponse dto = new DocumentoResponse();
        dto.setId(documento.getId());
        dto.setTipoDocumento(documento.getTipoDocumento());
        // Retorna nome decifrado se disponivel, senao nome legado
        dto.setNomeArquivo(documento.getNomeArquivoOriginal() != null
                ? documento.getNomeArquivoOriginal()
                : documento.getNomeArquivo());
        dto.setArquivoPath(documento.getArquivoPath());
        dto.setContentType(documento.getContentType());
        dto.setStatusDocumento(documento.getStatusDocumento());
        dto.setDataPostagem(documento.getDataPostagem());
        dto.setDataDownloadContratante(documento.getDataDownloadContratante());
        dto.setDataStatusAtualizado(documento.getDataStatusAtualizado());
        dto.setContratadaNome(
                documento.getContratada() != null ? documento.getContratada().getNome() : null);
        dto.setFuncionarioNome(
                documento.getFuncionario() != null
                        ? documento.getFuncionario().getNomeCompleto() : null);
        return dto;
    }

    public record DownloadPayload(Resource resource, String filename, String contentType) {}
}
