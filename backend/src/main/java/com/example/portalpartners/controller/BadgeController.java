package com.example.portalpartners.controller;

import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badge")
@RequiredArgsConstructor
public class BadgeController {
    private final DocumentoRepository documentoRepository;

    @GetMapping("/nao-analisados")
    public long countNaoAnalisados() {
        return documentoRepository.countByStatus(StatusDocumento.valueOf("POSTADO"));
    }
}
