package com.example.portalpartners.controller;

import com.example.portalpartners.dto.AuthRequest;
import com.example.portalpartners.dto.AuthResponse;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.UsuarioRepository;
import com.example.portalpartners.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail());

        if (usuario == null || !passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            System.out.println("Chegou aqui?");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtService.generateToken(usuario.getEmail(), usuario.getTipo());
        return ResponseEntity.ok(new AuthResponse(token, usuario.getTipo()));
    }
}
