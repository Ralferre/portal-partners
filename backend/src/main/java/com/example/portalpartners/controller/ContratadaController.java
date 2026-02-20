package com.example.portalpartners.controller;

import com.example.portalpartners.dto.*;
import com.example.portalpartners.service.FuncionarioService;
import com.example.portalpartners.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contratadas")
//@PreAuthorize("hasRole('CONTRATADA')")
@RequiredArgsConstructor
@Transactional
public class ContratadaController {

    private final FuncionarioService funcionarioService;
    private final UsuarioLogadoService usuarioLogadoService;

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @GetMapping("/funcionario")
    public FuncionarioResponse buscarFuncionarioPorNome(
            @RequestParam String nomeCompleto
    ) {
        return funcionarioService.buscarFuncionarioPorNomeCompleto(nomeCompleto);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @PostMapping("/funcionarios")
    public FuncionarioResponse criarFuncionario(
            @RequestBody CreateFuncionarioRequest request
    ) {
        return funcionarioService.criar(request);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @GetMapping("/list-funcionarios")
    public List<FuncionarioResponse> listarPaginado(
            @RequestParam(defaultValue = "0") int page
    ) {
        return funcionarioService.listar();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @GetMapping("/funcionarios")
    public List<FuncionarioResponse> listar() {
        return funcionarioService.listar();
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @GetMapping("/funcionarios/paged")
    public Page<FuncionarioResponse> listarPaginadoV2(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return funcionarioService.listarPaginado(page, size);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @DeleteMapping("/funcionarios/{id}")
    public void deletarFuncionario(@PathVariable Long id) {
        funcionarioService.deletarFuncionario(id);
    }
}
