package com.example.portalpartners.service;

import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import com.example.portalpartners.util.EmailDomainUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrganizacaoAcessoService {
    private final ContratanteRepository contratanteRepository;
    private final UsuarioRepository usuarioRepository;

    public Contratante resolverContratante(Usuario usuario) {
        if (usuario.getRole() != Role.CONTRATANTE) {
            throw new BusinessRulesException("Usuario nao possui perfil CONTRATANTE");
        }

        if (usuario.getContratante() != null) {
            return usuario.getContratante();
        }

        return resolverContratantePorDominio(usuario.getEmail());
    }

    public Contratada resolverContratada(Usuario usuario) {
        if (usuario.getRole() != Role.CONTRATADA) {
            throw new BusinessRulesException("Usuario nao possui perfil CONTRATADA");
        }

        if (usuario.getContratada() != null) {
            return usuario.getContratada();
        }

        return resolverContratadaPorDominio(usuario.getEmail());
    }

    private Contratante resolverContratantePorDominio(String email) {
        String domain = EmailDomainUtils.extractDomain(email);
        return contratanteRepository.findByDominioEmail(domain)
                .orElseThrow(() -> new ResourceNotFoundException("Contratante nao encontrada para o dominio do usuario"));
    }

    private Contratada resolverContratadaPorDominio(String email) {
        String domainSuffix = EmailDomainUtils.extractDomain(email);
        List<Contratada> matches = usuarioRepository
                .findByRoleAndEmailEndingWithIgnoreCase(Role.CONTRATADA, "@" + domainSuffix)
                .stream()
                .map(Usuario::getContratada)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (matches.isEmpty()) {
            throw new ResourceNotFoundException("Contratada nao encontrada para o dominio do usuario");
        }

        return matches.get(0);
    }
}
