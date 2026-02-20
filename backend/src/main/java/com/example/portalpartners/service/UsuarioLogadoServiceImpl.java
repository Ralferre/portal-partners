package com.example.portalpartners.service;

import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioLogadoServiceImpl implements UsuarioLogadoService{
    private final UsuarioRepository usuarioRepository;
    private final ContratanteRepository contratanteRepository;
    private final ContratadaRepository contratadaRepository;

    @Override
    public Usuario getUsuario() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessRulesException("Usuário não autenticado");
        }

        String email = authentication.getName();

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    @Override
    public Contratante getContratanteLogada() {

        Usuario usuario = getUsuario();

        if (usuario.getRole() != Role.CONTRATANTE) {
            throw new BusinessRulesException("Usuário não é CONTRATANTE");
        }

        return contratanteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFoundException("Contratante não encontrada"));
    }

    @Override
    public Contratada getContratadaLogada() {

        Usuario usuario = getUsuario();

        if (usuario.getRole() != Role.CONTRATADA) {
            throw new BusinessRulesException("Usuário não é CONTRATADA");
        }

        return contratadaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new BusinessRulesException("Contratada não encontrada"));
    }

    @Override
    public boolean isAdmin() {
        return getUsuario().getRole() == Role.ADMIN;
    }
}
