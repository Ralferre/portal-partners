package com.example.portalpartners.controller;

import com.example.portalpartners.dto.*;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.service.FuncionarioService;
import com.example.portalpartners.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/funcionario")
    public FuncionarioResponse buscarFuncionarioPorNome(
            @RequestParam String nomeCompleto
    ) {
        return funcionarioService.buscarFuncionarioPorNomeCompleto(nomeCompleto);
    }

    @Transactional
    @PostMapping("/funcionarios")
    public FuncionarioResponse criarFuncionario(
            @RequestBody CreateFuncionarioRequest request
    ) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
            throw new BusinessRulesException("Usuário sem permissão");
        }
        return funcionarioService.criar(request);
    }

    @GetMapping("/list-funcionarios")
    public List<FuncionarioResponse> listarPaginado(
            @RequestParam(defaultValue = "0") int page
    ) {
        return funcionarioService.listar();
    }
}
