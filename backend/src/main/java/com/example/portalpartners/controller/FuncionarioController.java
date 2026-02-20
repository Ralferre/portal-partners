package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateFuncionarioRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.service.FuncionarioService;
import com.example.portalpartners.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
//@PreAuthorize("hasRole('CONTRATADA')")
@RequiredArgsConstructor
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final UsuarioLogadoService usuarioLogadoService;

//    @GetMapping
//    public List<FuncionarioResponse> listarPaginado(
//            @RequestParam(defaultValue = "0") int page
//    ) {
//        return funcionarioService.listar();
//    }
//    @Transactional
//    @PostMapping("/contratada")
//    public FuncionarioResponse criar(@RequestBody CreateFuncionarioRequest request) {
//
//        Usuario usuario = usuarioLogadoService.getUsuario();
//        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
//            throw new RuntimeException("Usuário sem permissão");
//        } else {
//            return funcionarioService.criar(request);
//        }
//    }

    @GetMapping("/funcionario/{nomeCompleto}")
    public FuncionarioResponse buscarPorNomeCompleto(
            @PathVariable String nomeCompleto
    ) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
            throw new RuntimeException("Usuário sem permissão");
        } else {
            return funcionarioService.buscarFuncionarioPorNomeCompleto(nomeCompleto);
        }
    }

}

