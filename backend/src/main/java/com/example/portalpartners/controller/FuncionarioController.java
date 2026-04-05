package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateFuncionarioRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.service.FuncionarioService;
import com.example.portalpartners.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final UsuarioLogadoService usuarioLogadoService;

    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATADA', 'ROLE_ADMIN')")
    @GetMapping("/funcionario/{nomeCompleto}")
    public FuncionarioResponse buscarPorNomeCompleto(
            @PathVariable String nomeCompleto
    ) {
        return funcionarioService.buscarFuncionarioPorNomeCompleto(nomeCompleto);
    }

}

