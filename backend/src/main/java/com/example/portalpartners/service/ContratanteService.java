package com.example.portalpartners.service;

import com.example.portalpartners.dto.ContratanteResponse;
import com.example.portalpartners.dto.CreateContratanteRequest;
import com.example.portalpartners.exceptions.ResourceNotFopundException;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContratanteService {
    private final ContratanteRepository contratanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<ContratanteResponse> listarPaginado(int page) {
        return contratanteRepository.findAll(
                PageRequest.of(page, 10)
        ).map(ContratanteResponse::fromEntity);
    }

    public ContratanteResponse criar(CreateContratanteRequest request) {

        Usuario usuario = Usuario.builder()
                .email(request.email())
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.CONTRATANTE)
                .build();

        usuarioRepository.save(usuario);

        Contratante contratante = Contratante.builder()
                .nome(request.nome())
                .usuario(usuario)
                .build();

        contratanteRepository.save(contratante);

        return ContratanteResponse.fromEntity(contratante);
    }

    public void buscarPorNome(String nome) {
        Contratante contratante = contratanteRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFopundException("Contratante não encontrada"));

        contratanteRepository.findByNome(contratante.getNome());
    }

    public void removerPorNome(String nome) {
        Contratante contratante = contratanteRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFopundException("Contratante não encontrada"));

        contratanteRepository.delete(contratante);
    }
}
