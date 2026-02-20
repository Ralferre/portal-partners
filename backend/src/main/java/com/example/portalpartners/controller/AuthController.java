package com.example.portalpartners.controller;

import com.example.portalpartners.dto.*;
import com.example.portalpartners.service.AuthService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody AuthRequest request
    ) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            return ResponseEntity.ok(authService.forgotPassword(request));
        } catch (MessagingException e) {
            return ResponseEntity.internalServerError().body("Erro ao enviar email.");
        }
    }

    @PostMapping("/reset-password")
        public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
            return ResponseEntity.ok(authService.resetPassword(request));
        }
}
