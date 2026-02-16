package com.example.portalpartners.service;

import com.example.portalpartners.exceptions.BusinessRulersException;
import com.example.portalpartners.exceptions.ResourceNotFopundException;
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
            throw new BusinessRulersException("Usuário não autenticado");
        }

        String email = authentication.getName();

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFopundException("Usuário não encontrado"));
    }

    @Override
    public Contratante getContratanteLogada() {

        Usuario usuario = getUsuario();

        if (usuario.getRole() != Role.CONTRATANTE) {
            throw new BusinessRulersException("Usuário não é CONTRATANTE");
        }

        return contratanteRepository.findByUsuario(usuario)
                .orElseThrow(() -> new ResourceNotFopundException("Contratante não encontrada"));
    }

    @Override
    public Contratada getContratadaLogada() {

        Usuario usuario = getUsuario();

        if (usuario.getRole() != Role.CONTRATADA) {
            throw new BusinessRulersException("Usuário não é CONTRATADA");
        }

        return contratadaRepository.findByUsuario(usuario)
                .orElseThrow(() -> new BusinessRulersException("Contratada não encontrada"));
    }

    @Override
    public boolean isAdmin() {
        return getUsuario().getRole() == Role.ADMIN;
    }
}
