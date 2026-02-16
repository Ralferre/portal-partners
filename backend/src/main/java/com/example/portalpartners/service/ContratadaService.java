package com.example.portalpartners.service;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.exceptions.ResourceNotFopundException;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContratadaService {
    private final ContratadaRepository contratadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogadoService usuarioLogadoService;

    public Page<ContratadaResponse> listarPaginado(int page) {
        Contratante contratante = usuarioLogadoService.getContratanteLogada();
        return contratadaRepository
                .findByContratante(
                        contratante,
                        PageRequest.of(page, 10)
                )
                .map(ContratadaResponse::fromEntity);
    }

    public Page<ContratadaResponse> listarPorContratante(int page) {

        Contratante contratante = usuarioLogadoService.getContratanteLogada();

        return contratadaRepository
                .findByContratante(
                        contratante,
                        PageRequest.of(page, 10)
                )
                .map(ContratadaResponse::fromEntity);
    }

    public ContratadaResponse buscarPorNome(String nome) {
        Contratante contratante = usuarioLogadoService.getContratanteLogada();
        return contratadaRepository
                .findByContratanteAndNomeContainingIgnoreCase(contratante, nome)
                .map(ContratadaResponse::fromEntity)
                .orElseThrow(() ->
                        new ResourceNotFopundException("Contratada não encontrada"));
    }

    @Transactional
    public ContratadaResponse criar(CreateContratadaRequest request) {

        Contratante contratante = usuarioLogadoService.getContratanteLogada();

        Usuario usuario = Usuario.builder()
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.CONTRATADA)
                .build();

        usuarioRepository.save(usuario);

        Contratada contratada = Contratada.builder()
                .nome(request.nome())
                .cnpj(request.cnpj())
                .numeroContrato(request.numeroContrato())
                .numeroPedido(request.numeroPedido())
                .contratante(contratante)
                .usuario(usuario)
                .build();

        contratadaRepository.save(contratada);

        return ContratadaResponse.fromEntity(contratada);
    }

    public void removerPorNome(String nome) {
        Contratada contratada = contratadaRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFopundException("Contratada não encontrada"));

        contratadaRepository.delete(contratada);
    }
}
