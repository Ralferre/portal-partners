package com.example.portalpartners.controller;

import com.example.portalpartners.dto.*;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.service.DocumentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

    private final DocumentoRepository documentoRepository;
    private final DocumentoService documentoService;

    // =========================================================================
    // ARQUITETURA ZERO-COPY — Presigned URLs
    // =========================================================================

    /**
     * Solicita presigned PUT URL para upload direto do cliente ao MinIO.
     * Os bytes do documento NUNCA passam pelo backend.
     * Valida LGPD antes de gerar a URL.
     */
    @PostMapping("/solicitar-upload")
    @PreAuthorize("hasAuthority('ROLE_CONTRATADA')")
    public ResponseEntity<SolicitarUploadResponse> solicitarUpload(
            @Valid @RequestBody SolicitarUploadRequest request) {
        return ResponseEntity.ok(documentoService.solicitarUpload(request));
    }

    /**
     * Confirma a conclusao do upload direto ao MinIO.
     * Chamado pelo frontend apos o PUT bem-sucedido na presigned URL.
     */
    @PostMapping("/{documentoId}/confirmar-upload")
    @PreAuthorize("hasAuthority('ROLE_CONTRATADA')")
    public ResponseEntity<DocumentoResponse> confirmarUpload(@PathVariable Long documentoId) {
        return ResponseEntity.ok(documentoService.confirmarUpload(documentoId));
    }

    /**
     * Solicita presigned GET URL para download direto do cliente ao MinIO.
     * Os bytes do documento NUNCA passam pelo backend.
     * Gera evento de auditoria assincrono.
     */
    @GetMapping("/{id}/solicitar-download")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    public ResponseEntity<SolicitarDownloadResponse> solicitarDownload(@PathVariable Long id) {
        return ResponseEntity.ok(documentoService.solicitarDownload(id));
    }

    // =========================================================================
    // ENDPOINTS LEGADOS (retrocompatibilidade)
    // =========================================================================

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<DocumentoResponse> uploadDocumento(
            @ModelAttribute CreateDocumentoRequest dto) {
        DocumentoResponse response = documentoService.uploadDocumento(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        DocumentoService.DownloadPayload payload = documentoService.download(id);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + payload.filename() + "\"");

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(payload.contentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(payload.resource());
    }

    // =========================================================================
    // STATUS, FILTROS, CONTAGEM, TIPOS, EXCLUSAO
    // =========================================================================

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_ADMIN')")
    public ResponseEntity<DocumentoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(documentoService.updateStatus(id, request.statusDocumento()));
    }

    @GetMapping("/novos/count")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE')")
    public ResponseEntity<Long> countNovos() {
        return ResponseEntity.ok(documentoService.countNovosDocumentosParaContratante());
    }

    @GetMapping("/tipos")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    public ResponseEntity<List<TipoDocumento>> listarTipos() {
        return ResponseEntity.ok(documentoService.listarTiposDocumento());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @GetMapping
    public Page<DocumentoResponse> filtrar(
            @RequestParam(required = false) String contratada,
            @RequestParam(required = false) String funcionario,
            @RequestParam(required = false) TipoDocumento tipo,
            @RequestParam(required = false) StatusDocumento status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return documentoService.filtrar(contratada, funcionario, tipo, status,
                PageRequest.of(page, size));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        documentoService.deletarDocumento(id);
        return ResponseEntity.noContent().build();
    }

    // mapper auxiliar para endpoints legados
    private DocumentoResponse toResponse(Documento d) {
        DocumentoResponse dto = new DocumentoResponse();
        dto.setId(d.getId());
        dto.setTipoDocumento(d.getTipoDocumento());
        dto.setNomeArquivo(d.getNomeArquivoOriginal() != null
                ? d.getNomeArquivoOriginal() : d.getNomeArquivo());
        dto.setStatusDocumento(d.getStatusDocumento());
        dto.setDataPostagem(d.getDataPostagem());
        dto.setContratadaNome(d.getContratada() != null ? d.getContratada().getNome() : null);
        dto.setFuncionarioNome(
                d.getFuncionario() != null ? d.getFuncionario().getNomeCompleto() : null);
        return dto;
    }
}
