package com.example.portalpartners.controller;

import com.example.portalpartners.model.Documento;
import com.example.portalpartners.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {
    private final DocumentoRepository documentoRepository;

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
}
