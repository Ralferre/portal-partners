package com.example.portalpartners.controller;

import com.example.portalpartners.dto.ContratanteResponse;
import com.example.portalpartners.dto.CreateContratanteRequest;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.service.ContratanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final ContratanteService contratanteService;

    @GetMapping("/contratantes")
    public Page<ContratanteResponse> listar(
            @RequestParam(defaultValue = "0") int page
    ) {
        return contratanteService.listarPaginado(page);
    }

    @GetMapping("/contratante/{nome}")
    public void buscarPorNome(@PathVariable String nome) {
        contratanteService.buscarPorNome(nome);
    }

    @PostMapping("/contratante")
    public ContratanteResponse criar(
            @RequestBody CreateContratanteRequest request
    ) {
        return contratanteService.criar(request);
    }

    @DeleteMapping("/contratante/{nome}")
    public void remover(@PathVariable String nome) {
        contratanteService.removerPorNome(nome);
    }
}
