package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateDocumento;
import com.example.portalpartners.dto.DocumentoResponse;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.TipoDocumento;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.service.DocumentoService;
import com.example.portalpartners.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {
    private final DocumentoRepository documentoRepository;
    private final MinioService minioService;
    private final DocumentoService documentoService;

    @GetMapping
    public Page<Documento> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return documentoRepository.findAll(pageable);
    }

    @GetMapping("/ultimos")
    public List<Documento> ultimos10() {
        return documentoRepository.findTop10ByOrderByDataPostagemDesc();
    }

    @PostMapping
    public ResponseEntity<Documento> cadastrar(@RequestBody Documento documento) {
        return ResponseEntity.ok(documentoRepository.save(documento));
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentoResponse> uploadDocumento(@ModelAttribute CreateDocumento dto) {
        Documento documento = documentoService.uploadDocumento(dto);
        DocumentoResponse response = new DocumentoResponse();
        response.setId(documento.getId());
        response.setTipoDocumento(documento.getTipoDocumento());
        response.setNomeArquivo(documento.getNomeArquivo());
        response.setStatus(documento.getStatusDocumento());
        response.setDataPostagem(documento.getDataPostagem());
        response.setContratadaId(dto.getContratadaId());
        response.setFuncionarioId(dto.getFuncionarioId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/contratada/{contratadaId}")
    public ResponseEntity<List<DocumentoResponse>> listarPorContratada(@PathVariable Long contratadaId) {
        List<Documento> documentos = documentoService.findByContratadaUuid(contratadaId);
        List<DocumentoResponse> dtos = documentos.stream()
                .map(d -> {
                    DocumentoResponse dto = new DocumentoResponse();
                    dto.setId(d.getId());
                    dto.setTipoDocumento(d.getTipoDocumento());
                    dto.setNomeArquivo(d.getNomeArquivo());
                    dto.setStatus(d.getStatusDocumento());
                    dto.setDataPostagem(d.getDataPostagem());
                    dto.setContratadaId(d.getContratada() != null ? d.getContratada().getId() : null);
                    dto.setFuncionarioId(d.getFuncionario() != null ? d.getFuncionario().getId() : null);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/funcionario/{funcionarioId}")
    public ResponseEntity<List<DocumentoResponse>> listarPorFuncionario(@PathVariable Long funcionarioId) {
        List<Documento> documentos = documentoService.findByFuncionarioId(funcionarioId);
        List<DocumentoResponse> dtos = documentos.stream()
                .map(d -> {
                    DocumentoResponse dto = new DocumentoResponse();
                    dto.setId(d.getId());
                    dto.setTipoDocumento(d.getTipoDocumento());
                    dto.setNomeArquivo(d.getNomeArquivo());
                    dto.setStatus(d.getStatusDocumento());
                    dto.setDataPostagem(d.getDataPostagem());
                    dto.setContratadaId(d.getContratada() != null ? d.getContratada().getId() : null);
                    dto.setFuncionarioId(d.getFuncionario() != null ? d.getFuncionario().getId() : null);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/contratada/{contratadaId}/tipo/{tipo}")
    public ResponseEntity<List<DocumentoResponse>> filtrarPorTipoEmpresa(
            @PathVariable Long contratadaId,
            @PathVariable TipoDocumento tipo) {
        List<Documento> documentos = documentoService.findByContratadaIdAndTipo(contratadaId, String.valueOf(tipo));
        List<DocumentoResponse> dtos = documentos.stream()
                .map(d -> {
                    DocumentoResponse dto = new DocumentoResponse();
                    dto.setId(d.getId());
                    dto.setTipoDocumento(d.getTipoDocumento());
                    dto.setNomeArquivo(d.getNomeArquivo());
                    dto.setStatus(d.getStatusDocumento());
                    dto.setDataPostagem(d.getDataPostagem());
                    dto.setContratadaId(d.getContratada() != null ? d.getContratada().getId() : null);
                    dto.setFuncionarioId(d.getFuncionario() != null ? d.getFuncionario().getId() : null);
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
