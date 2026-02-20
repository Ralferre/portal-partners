package com.example.portalpartners.controller;

import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/badge")
@RequiredArgsConstructor
public class BadgeController {
    private final DocumentoRepository documentoRepository;

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_ADMIN')")
    @GetMapping("/pendentes")
    public long countNaoAnalisados() {
        return documentoRepository.countByStatusDocumento(StatusDocumento.valueOf("PENDENTE"));
    }
}
