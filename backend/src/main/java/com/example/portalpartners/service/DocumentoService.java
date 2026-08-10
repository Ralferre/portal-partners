package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.documento.DocumentoSpecification;
import com.example.portalpartners.dto.*;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.exceptions.ConflictException;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import com.example.portalpartners.model.DownloadToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final DownloadTokenService downloadTokenService;

    /** URL publica desta API: o link de download precisa ser absoluto. */
    @Value("${app.api-url}")
    private String apiUrl;

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
        // "POSTADO" representa upload solicitado e aguardando validacao/confirmacao final.
        documento.setStatusDocumento(StatusDocumento.POSTADO);
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

        resolverReferencia(documento, request);

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
        validarEstadoParaConfirmacao(documento);
        validarObjetoUploadNoStorage(documento);
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

        String key = resolverChaveDownloadDisponivel(documento);

        // O badge de "novos" passa a ser marcado no download efetivo, e nao
        // aqui: solicitar o link e desistir nao conta mais como baixado.
        boolean marcaDownloadContratante = usuario.getRole() == Role.CONTRATANTE
                && documento.getDataDownloadContratante() == null;

        DownloadToken token = downloadTokenService.emitir(
                documento.getId(), usuario.getId(), key, marcaDownloadContratante);

        String downloadUrl = apiUrl.replaceAll("/+$", "")
                + "/api/documentos/download/" + token.getToken();

        // Nome exibido: usa nomeArquivoOriginal (decifrado pelo converter) se disponivel
        String nomeExibido = documento.getNomeArquivoOriginal() != null
                ? documento.getNomeArquivoOriginal()
                : documento.getNomeArquivo();

        return new SolicitarDownloadResponse(downloadUrl, nomeExibido,
                documento.getContentType(), downloadTokenService.validadeSegundos());
    }

    /**
     * Serve o arquivo a partir de um token de uso unico.
     *
     * A autorizacao ja foi verificada quando o token foi emitido (usuario
     * autenticado e `validarPermissaoDownload`). Aqui o token e a credencial:
     * consumi-lo com sucesso e o que autoriza a entrega dos bytes.
     */
    @Transactional
    public DownloadPayload downloadPorToken(String token, String ip) {
        DownloadToken autorizacao = downloadTokenService.consumir(token, ip);

        Documento documento = documentoRepository.findById(autorizacao.getDocumentoId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado"));

        if (autorizacao.isMarcaDownloadContratante()
                && documento.getDataDownloadContratante() == null) {
            documento.setDataDownloadContratante(LocalDateTime.now());
            documentoRepository.save(documento);
        }

        String key = autorizacao.getObjectKey();

        InputStream stream = minioService.getObjectStream(key);
        Resource resource = new InputStreamResource(stream);

        String contentType = documento.getContentType();
        if (contentType == null || contentType.isBlank()) {
            try {
                contentType = minioService.statObject(key).contentType();
            } catch (Exception e) {
                contentType = "application/octet-stream";
            }
        }

        String nomeExibido = documento.getNomeArquivoOriginal() != null
                ? documento.getNomeArquivoOriginal()
                : documento.getNomeArquivo();

        return new DownloadPayload(resource, nomeExibido, contentType);
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
            Contratada contratada = resolverContratadaNoEscopoDaLogada(dto.contratadaId());
            documento.setContratada(contratada);
            documento.setFuncionario(null);
        } else if (dto.tipoReferenciaDocumento() == TypeReferenceFile.FUNCIONARIO) {
            if (dto.funcionarioId() == null) {
                throw new BusinessRulesException("Funcionário deve ser informado");
            }
            Funcionario funcionario = resolverFuncionarioNoEscopoDaLogada(dto.funcionarioId());
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

        String key = resolverChaveDownloadDisponivel(documento);

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

    private void resolverReferencia(Documento documento, SolicitarUploadRequest request) {
        if (request.tipoReferencia() == TypeReferenceFile.CONTRATADA) {
            Contratada contratada = resolverContratadaNoEscopoDaLogada(request.contratadaId());
            documento.setContratada(contratada);
        } else if (request.tipoReferencia() == TypeReferenceFile.FUNCIONARIO) {
            if (request.funcionarioId() == null) {
                throw new BusinessRulesException("FuncionarioId deve ser informado");
            }
            Funcionario funcionario = resolverFuncionarioNoEscopoDaLogada(request.funcionarioId());
            documento.setFuncionario(funcionario);
            documento.setContratada(funcionario.getContratada());
        } else {
            throw new BusinessRulesException("Tipo de referencia invalido");
        }
    }

    private Contratada resolverContratadaNoEscopoDaLogada(Long contratadaIdInformada) {
        Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();
        if (contratadaIdInformada == null) {
            return contratadaLogada;
        }

        Contratada contratadaInformada = contratadaRepository.findById(contratadaIdInformada)
                .orElseThrow(() -> new BusinessRulesException("Contratada nao encontrada"));

        if (!contratadaInformada.getId().equals(contratadaLogada.getId())) {
            throw new ForbiddenException(
                    "Voce nao pode enviar documento para outra contratada");
        }
        return contratadaInformada;
    }

    private Funcionario resolverFuncionarioNoEscopoDaLogada(Long funcionarioId) {
        Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .orElseThrow(() -> new BusinessRulesException("Funcionario nao encontrado"));

        if (funcionario.getContratada() == null
                || !funcionario.getContratada().getId().equals(contratadaLogada.getId())) {
            throw new ForbiddenException(
                    "Voce nao pode enviar documento para funcionario de outra contratada");
        }
        return funcionario;
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

    private void validarObjetoUploadNoStorage(Documento documento) {
        if (documento.getObjectKey() == null || documento.getObjectKey().isBlank()) {
            throw new BusinessRulesException("Documento sem objectKey para validacao de upload");
        }

        try {
            var stat = minioService.statObject(documento.getObjectKey());

            long tamanhoReal = stat.size();
            String contentTypeReal = normalizarContentType(stat.contentType());
            String contentTypeEsperado = normalizarContentType(documento.getContentType());
            String etag = stat.etag();

            validarTamanho(tamanhoReal);
            validarContentType(contentTypeReal);
            validarEtag(etag);

            if (documento.getTamanhoBytes() == null || documento.getTamanhoBytes() != tamanhoReal) {
                throw new BusinessRulesException(
                        "Tamanho do arquivo enviado difere do solicitado");
            }

            if (contentTypeEsperado == null || !contentTypeEsperado.equals(contentTypeReal)) {
                throw new BusinessRulesException(
                        "Tipo real do arquivo difere do tipo informado na solicitacao");
            }

            // Normaliza metadados persistidos para refletir o objeto real armazenado.
            documento.setContentType(contentTypeReal);
            documento.setTamanhoBytes(tamanhoReal);
        } catch (BusinessRulesException e) {
            throw e;
        } catch (RuntimeException e) {
            String mensagem = e.getMessage() != null ? e.getMessage() : "";
            if (mensagem.contains("NoSuchKey")
                    || mensagem.contains("Object does not exist")
                    || mensagem.contains("object does not exist")) {
                throw new BusinessRulesException(
                        "Objeto de upload nao encontrado no storage. Reenvie o arquivo.");
            }
            throw new BusinessRulesException(
                    "Falha ao validar integridade do arquivo no storage. Reenvie o upload e tente novamente.");
        }
    }

    private String normalizarContentType(String contentType) {
        if (contentType == null) return null;
        String normalized = contentType.trim().toLowerCase();
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private void validarEstadoParaConfirmacao(Documento documento) {
        if (documento.getStatusDocumento() != StatusDocumento.POSTADO) {
            throw new ConflictException(
                    "Documento nao esta pendente de confirmacao de upload");
        }
    }

    private void validarEtag(String etag) {
        if (etag == null || etag.isBlank()) {
            throw new BusinessRulesException(
                    "Integridade do objeto invalida: ETag ausente no storage");
        }
        String normalized = etag.trim().replace("\"", "");
        // Para arquivos <= 10MB (single part), MinIO/S3 retorna ETag MD5 (32 hex chars).
        if (!normalized.matches("^[a-fA-F0-9]{32}$")) {
            throw new BusinessRulesException(
                    "Integridade do objeto invalida: ETag fora do padrao esperado");
        }
    }

    private String resolverChaveDownloadDisponivel(Documento documento) {
        if (documento.getStatusDocumento() == StatusDocumento.POSTADO) {
            throw new BusinessRulesException(
                    "Documento ainda nao foi confirmado no storage e nao pode ser baixado");
        }

        String[] candidatas = { documento.getObjectKey(), documento.getArquivoPath() };
        RuntimeException ultimaFalha = null;

        for (String key : candidatas) {
            if (key == null || key.isBlank()) {
                continue;
            }
            try {
                minioService.statObject(key);
                return key;
            } catch (RuntimeException e) {
                ultimaFalha = e;
            }
        }

        throw new BusinessRulesException(
                "Arquivo nao encontrado no storage para este documento. Reenvie o arquivo.");
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
