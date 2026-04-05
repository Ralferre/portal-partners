package com.example.portalpartners.controller;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;
import com.example.portalpartners.dto.CreateUsuarioContratanteRequest;
import com.example.portalpartners.dto.UpdateContratadaRequest;
import com.example.portalpartners.dto.UsuarioDTO;
import com.example.portalpartners.service.ContratadaService;
import com.example.portalpartners.service.ContratanteService;
import com.example.portalpartners.service.ContratanteUsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/contratantes")
@RequiredArgsConstructor
@Transactional
public class ContratanteController {
    private final ContratanteService contratanteService;
    private final ContratadaService contratadaService;
    private final ContratanteUsuarioService contratanteUsuarioService;

    @Transactional
    @PreAuthorize("hasRole('CONTRATANTE')")
    @PostMapping("/contratada")
    public ContratadaResponse criar(
            @Valid @RequestBody CreateContratadaRequest request
    ) {
        return contratadaService.criar(request);
    }

    @GetMapping("/contratada")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public ContratadaResponse buscarPorNome(
            @RequestParam String nome
    ) {
        return contratadaService.buscarPorNome(nome);
    }

    @GetMapping("/contratadas")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public Page<ContratadaResponse> listarPaginado(
            @RequestParam(defaultValue = "0") int page) {
        return contratadaService.listarPorContratante(page);
    }

    @DeleteMapping("/contratadas/{id}")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public void deletarContratada(@PathVariable Long id) {
        contratadaService.deletarContratada(id);
    }

    @PutMapping("/contratadas/{id}")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public ContratadaResponse atualizarContratada(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContratadaRequest request
    ) {
        return contratadaService.atualizar(id, request);
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public List<UsuarioDTO> listarUsuarios() {
        return contratanteUsuarioService.listarUsuariosDaContratanteLogada();
    }

    @PostMapping("/usuarios")
    @PreAuthorize("hasRole('CONTRATANTE')")
    public UsuarioDTO criarUsuario(
            @Valid @RequestBody CreateUsuarioContratanteRequest request
    ) {
        return contratanteUsuarioService.criarUsuarioParaContratanteLogada(request);
    }
}
