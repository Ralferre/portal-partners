package com.example.portalpartners.controller;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.service.ContratadaService;
import com.example.portalpartners.service.ContratanteService;
import com.example.portalpartners.service.UsuarioLogadoServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/contratantes")
@RequiredArgsConstructor
@Transactional
public class ContratanteController {
    private final ContratanteService contratanteService;
    private final UsuarioLogadoServiceImpl usuarioLogadoService;
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
    @PostMapping("/contratada")
    public ContratadaResponse criar(
            @RequestBody CreateContratadaRequest request
    ) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATADA) {
            throw new RuntimeException("Usuário sem permissão");
        } else {
            return contratadaService.criar(request);
        }
    }

    @GetMapping("/contratada")
    public ContratadaResponse buscarPorNome(
            @RequestParam String nome
    ) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATADA) {
            throw new RuntimeException("Usuário sem permissão");
        } else {
            return contratadaService.buscarPorNome(nome);
        }
    }

    @GetMapping("/contratadas")
    public Page<ContratadaResponse> listarPaginado(
            @RequestParam(defaultValue = "0") int page) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATADA) {
            throw new RuntimeException("Usuário sem permissão");
        } else {
            return contratadaService.listarPorContratante(page);
        }
    }
}
