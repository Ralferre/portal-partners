package com.example.portalpartners.controller;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;
import com.example.portalpartners.service.ContratadaService;
import com.example.portalpartners.service.ContratanteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/contratantes")
@RequiredArgsConstructor
@Transactional
public class ContratanteController {
    private final ContratanteService contratanteService;
    private final ContratadaService contratadaService;

//    @GetMapping("/contratantes")
//    public Page<ContratanteResponse> listar(
//            @RequestParam(defaultValue = "0") int page
//    ) {
//        return contratanteService.listarPaginado(page);
//    }

//    @Transactional
//    @PostMapping("/contratante")
//    public ContratanteResponse criar(
//            @RequestBody CreateContratanteRequest request
//    ) {
//        Usuario usuario = usuarioLogadoService.getUsuario();
//        if (usuario.getRole() == Role.CONTRATANTE || usuario.getRole() == Role.CONTRATADA) {
//            throw new RuntimeException("Usuário sem permissão");
//        } else {
//            return contratanteService.criar(request);
//        }
//    }

//    @Transactional
//    @DeleteMapping("/contratante/{nome}")
//    public void remover(@PathVariable String nome) {
//        Usuario usuario = usuarioLogadoService.getUsuario();
//        if (usuario.getRole() != Role.CONTRATANTE) {
//            throw new BusinessRulersException("Usuário sem permissão");
//        }
//
//        contratanteService.removerPorNome(nome);
//    }

    @Transactional
    @PreAuthorize("hasRole('CONTRATANTE')")
    @PostMapping("/contratada")
    public ContratadaResponse criar(
            @RequestBody CreateContratadaRequest request
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
}
