package com.example.portalpartners.service;

import com.example.portalpartners.dto.*;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.ContratanteRepository;
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
    private final ContratanteRepository contratanteRepository;
    private final ContratadaRepository contratadaRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(AuthRequest authRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getEmail(), authRequest.getSenha())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        System.out.println("Testando:" + usuarioRepository.findByEmail(userDetails.getUsername()));

        Usuario usuario = usuarioRepository
                .findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        String token = jwtService.generateToken(usuario);

        Long perfilId = null;

        if (usuario.getRole() == Role.CONTRATANTE) {
            perfilId = contratanteRepository
                    .findByUsuarioId(usuario.getId()).get().getId();
        }

        if (usuario.getRole() == Role.CONTRATADA) {
            perfilId = contratadaRepository
                    .findByUsuarioId(usuario.getId()).get().getId();
        }

        return new AuthResponse(
                token,
                usuario.getEmail(),
                usuario.getRole(),
                perfilId
        );
    }

    public String forgotPassword(ForgotPasswordRequest request) throws MessagingException {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email não encontrado"));

        String token = UUID.randomUUID().toString();
        usuario.setResetToken(token);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 1);
        usuario.setResetTokenExpiration(cal.getTime());

        usuarioRepository.save(usuario);
        emailService.sendResetPasswordEmail(usuario.getEmail(), token);

        return "Email enviado.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (
                usuario.getResetToken() == null ||
                        !usuario.getResetToken().equals(request.token()) ||
                        usuario.getResetTokenExpiration().before(new Date())
        ) {
            throw new IllegalArgumentException("Token inválido ou expirado.");
        }

        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.setResetToken(null);
        usuario.setResetTokenExpiration(null);

        usuarioRepository.save(usuario);
        return "Senha alterada.";
    }
}
