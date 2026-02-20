package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateDocumentoRequest;
import com.example.portalpartners.dto.DocumentoResponse;
import com.example.portalpartners.dto.UpdateRequest;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.service.DocumentoService;
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

    private DocumentoResponse toResponse(Documento d) {
        DocumentoResponse dto = new DocumentoResponse();
        dto.setId(d.getId());
        dto.setTipoDocumento(d.getTipoDocumento());
        dto.setNomeArquivo(d.getNomeArquivo());
        dto.setStatusDocumento(d.getStatusDocumento());
        dto.setDataPostagem(d.getDataPostagem());

        dto.setContratadaNome(
                d.getContratada() != null ? d.getContratada().getNome() : null
        );

        dto.setFuncionarioNome(
                d.getFuncionario() != null ? d.getFuncionario().getNomeCompleto() : null
        );

        return dto;
    }

//

//    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
//    @GetMapping("/ultimos")
//    public List<DocumentoResponse> ultimos10() {
//        return documentoRepository.findTop10ByOrderByDataPostagemDesc()
//                .stream()
//                .map(this::toResponse)
//                .toList();
//    }
//
//    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
//    @PostMapping
//    public ResponseEntity<Documento> cadastrar(@RequestBody Documento documento) {
//        return ResponseEntity.ok(documentoRepository.save(documento));
//    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @PostMapping("/upload")
    public ResponseEntity<DocumentoResponse> uploadDocumento(@ModelAttribute CreateDocumentoRequest dto) {
//        DocumentoResponse response = DocumentoResponse.fromEntity(dto);
        DocumentoResponse response = documentoService.uploadDocumento(dto);
        return ResponseEntity.ok(response);
    }

//    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
//    @GetMapping("/contratada")
//    public List<DocumentoResponse> listarPorContratada(@RequestParam String nome) {
//        return documentoService.findByContratadaNome(nome)
//                .stream()
//                .map(this::toResponse)
//                .toList();
//    }

//    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
//    @GetMapping("/funcionario/nome/{funcionarioNome}")
//    public Page<DocumentoResponse> listarPorFuncionario(
//            @PathVariable String nome,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "20") int size) {
//        Page<Documento> pageEntity =
//                documentoService.findByFuncionarioNomeContainingIgnoreCase(
//                        nome, PageRequest.of(page, size));
//
//        return pageEntity.map(this::toResponse);
//    }

//    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
//    @GetMapping("/contratada/{contratadaNome}/tipo/{tipo}")
//    public List<DocumentoResponse> filtrarPorTipoEmpresa(
//            @PathVariable String contratadaNome,
//            @PathVariable TipoDocumento tipo) {
//
//        return documentoService.findByContratadaNomeAndTipo(contratadaNome, tipo)
//                .stream()
//                .map(this::toResponse)
//                .toList();
//    }

    @PutMapping("/status/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_ADMIN')")
    public ResponseEntity<DocumentoResponse> atualizarStatus(
            @PathVariable Long id,
            @RequestBody UpdateRequest request) {
        DocumentoResponse response = documentoService.updateStatus(id, request.statusDocumento());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable Long id) {
        DocumentoService.DownloadPayload payload = documentoService.download(id);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.filename() + "\"");

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return documentoService.filtrar(
                contratada,
                funcionario,
                tipo,
                status,
                PageRequest.of(page, size)
        );
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        documentoService.deletarDocumento(id);
        return ResponseEntity.noContent().build();
    }
}
