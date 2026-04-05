package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.dto.*;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.repository.UsuarioRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final OrganizacaoAcessoService organizacaoAcessoService;
    private final UsuarioLogadoService usuarioLogadoService;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Auditavel(acao = "LOGIN_SUCESSO", entidade = "Usuario")
    public AuthResponse login(AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getSenha())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        Usuario usuario = usuarioRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = jwtService.generateToken(usuario);
        Long perfilId = resolvePerfilIdByOrganizacao(usuario);

        return new AuthResponse(
                token,
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                perfilId,
                Boolean.TRUE.equals(usuario.getMustChangePassword())
        );
    }

    public String forgotPassword(ForgotPasswordRequest request) throws MessagingException {
        Usuario usuario = usuarioRepository.findByEmail(request.email()).orElse(null);

        // resposta genérica para não expor se o email existe
        if (usuario == null) {
            return "Se o email existir, enviaremos instruções.";
        }

        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        usuario.setResetTokenExpiration(cal.getTime());

        usuarioRepository.save(usuario);
        emailService.sendResetPasswordEmail(usuario.getEmail(), token);

        return "Se o email existir, enviaremos instruções.";
    }

    @Auditavel(acao = "SENHA_RESETADA", entidade = "Usuario")
    public String resetPassword(ResetPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (
                usuario.getResetToken() == null ||
                        !usuario.getResetToken().equals(request.token()) ||
                        usuario.getResetTokenExpiration() == null ||
                        usuario.getResetTokenExpiration().before(new Date())
        ) {
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiration(null);
        usuario.setMustChangePassword(false);

        usuarioRepository.save(usuario);
        return "Senha alterada.";
    }

    @Auditavel(acao = "PRIMEIRO_ACESSO_SENHA_ALTERADA", entidade = "Usuario")
    public String changePasswordFirstAccess(FirstAccessPasswordChangeRequest request) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        if (!Boolean.TRUE.equals(usuario.getMustChangePassword())) {
            throw new BusinessRulesException("Este usuario nao esta no fluxo de primeiro acesso");
        }

        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new BusinessRulesException("Senha atual invalida");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setMustChangePassword(false);
        usuarioRepository.save(usuario);
        return "Senha alterada com sucesso. Efetue novo login.";
    }

    private Long resolvePerfilIdByOrganizacao(Usuario usuario) {
        if (usuario.getRole() == Role.CONTRATANTE) {
            return organizacaoAcessoService.resolverContratante(usuario).getId();
        }

        if (usuario.getRole() == Role.CONTRATADA) {
            return organizacaoAcessoService.resolverContratada(usuario).getId();
        }

        return null;
    }
}
