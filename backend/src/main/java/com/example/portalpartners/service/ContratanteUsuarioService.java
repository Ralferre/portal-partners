package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.dto.CreateUsuarioContratanteRequest;
import com.example.portalpartners.dto.UsuarioDTO;
import com.example.portalpartners.exceptions.ConflictException;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.UsuarioRepository;
import com.example.portalpartners.util.EmailDomainUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContratanteUsuarioService {
    private final UsuarioLogadoService usuarioLogadoService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UsuarioDTO> listarUsuariosDaContratanteLogada() {
        Contratante contratante = usuarioLogadoService.getContratanteLogada();

        return usuarioRepository.findByContratanteIdOrderByIdAsc(contratante.getId())
                .stream()
                .filter(usuario -> usuario.getRole() == Role.CONTRATANTE)
                .map(usuario -> toDto(usuario, contratante))
                .toList();
    }

    @Auditavel(acao = "USUARIO_CONTRATANTE_CRIADO", entidade = "Usuario")
    @Transactional
    public UsuarioDTO criarUsuarioParaContratanteLogada(CreateUsuarioContratanteRequest request) {
        Contratante contratante = usuarioLogadoService.getContratanteLogada();

        String email = request.email().trim().toLowerCase();
        String dominioEmail = EmailDomainUtils.extractDomain(email);

        if (!dominioEmail.equalsIgnoreCase(contratante.getDominioEmail())) {
            throw new ConflictException("O domínio do e-mail deve ser o mesmo da organização contratante");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("Email já está em uso");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.CONTRATANTE)
                .mustChangePassword(true)
                .contratante(contratante)
                .build();

        Usuario saved = usuarioRepository.save(usuario);
        return toDto(saved, contratante);
    }

    private UsuarioDTO toDto(Usuario usuario, Contratante contratante) {
        UsuarioDTO dto = new UsuarioDTO();
        dto.setId(usuario.getId());
        dto.setNome(usuario.getNome());
        dto.setEmail(usuario.getEmail());
        dto.setMustChangePassword(Boolean.TRUE.equals(usuario.getMustChangePassword()));
        dto.setPrincipal(
                contratante.getUsuario() != null
                        && contratante.getUsuario().getId().equals(usuario.getId())
        );
        return dto;
    }
}
