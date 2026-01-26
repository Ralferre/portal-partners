package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateDocumentoRequest;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {
    private final DocumentoRepository documentoRepository;
    private final MinioService minioService;
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

    @GetMapping
    public Page<DocumentoResponse> listarPaginado(@RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size) {
        Page<Documento> documentos = documentoRepository.findAll(PageRequest.of(page, size));

        return documentos.map(this::toResponse);
    }

    @GetMapping("/ultimos")
    public List<DocumentoResponse> ultimos10() {
        return documentoRepository.findTop10ByOrderByDataPostagemDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<Documento> cadastrar(@RequestBody Documento documento) {
        return ResponseEntity.ok(documentoRepository.save(documento));
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentoResponse> uploadDocumento(@ModelAttribute CreateDocumentoRequest dto) {
        Documento documento = documentoService.uploadDocumento(dto);
        DocumentoResponse response = new DocumentoResponse();
        response.setId(documento.getId());
        response.setTipoDocumento(documento.getTipoDocumento());
        response.setStatusDocumento(documento.getStatusDocumento());
        response.setNomeArquivo(documento.getNomeArquivo());
        response.setDataPostagem(documento.getDataPostagem());
        response.setContratadaNome(documento.getContratada().getNome());
        response.setFuncionarioNome(documento.getFuncionario().getNomeCompleto());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/contratada/{nome}")
    public List<DocumentoResponse> listarPorContratada(@PathVariable String nome) {
        return documentoService.findByContratadaNome(nome)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/funcionario/nome/{funcionarioNome}")
    public Page<DocumentoResponse> listarPorFuncionario(
            @PathVariable String nome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Documento> pageEntity =
                documentoService.findByFuncionarioNomeContainingIgnoreCase(
                        nome, PageRequest.of(page, size));

        return pageEntity.map(this::toResponse);
    }

    @GetMapping("/contratada/{contratadaNome}/tipo/{tipo}")
    public List<DocumentoResponse> filtrarPorTipoEmpresa(
            @PathVariable String contratadaNome,
            @PathVariable TipoDocumento tipo) {

        return documentoService.findByContratadaNomeAndTipo(contratadaNome, tipo)
                .stream()
                .map(this::toResponse)
                .toList();
    }
}
