package com.example.portalpartners.service;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
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
                        new ResourceNotFoundException("Contratada não encontrada"));
    }

    @Transactional
    public ContratadaResponse criar(CreateContratadaRequest request) {

        Contratante contratante = usuarioLogadoService.getContratanteLogada();

        String cnpjNormalizado = normalizarCnpj(request.cnpj());

        Usuario usuario = Usuario.builder()
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.CONTRATADA)
                .build();

        usuarioRepository.save(usuario);

        Contratada contratada = Contratada.builder()
                .nome(request.nome())
                .cnpj(cnpjNormalizado)
                .numeroContrato(request.numeroContrato())
                .numeroPedido(request.numeroPedido())
                .contratante(contratante)
                .usuario(usuario)
                .build();

        contratadaRepository.save(contratada);

        return ContratadaResponse.fromEntity(contratada);
    }

    public Page<ContratadaResponse> listarTodas(int page) {
        return contratadaRepository.findAll(PageRequest.of(page, 10))
                .map(ContratadaResponse::fromEntity);
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("\\D", "");
    }

    @Transactional
    public void deletarContratada(Long id) {

        Contratada contratada = contratadaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contratada não encontrada"));

        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN) {
            contratadaRepository.delete(contratada);
            return;
        }

        Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();

        if (!contratada.getContratante().getId().equals(contratanteLogado.getId())) {
            throw new ForbiddenException("Você não tem permissão para deletar esta contratada");
        }

        contratadaRepository.delete(contratada);
    }

    public void removerPorNome(String nome) {
        Contratada contratada = contratadaRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Contratada não encontrada"));

        contratadaRepository.delete(contratada);
    }
}
