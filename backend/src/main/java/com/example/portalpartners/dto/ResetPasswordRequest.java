package com.example.portalpartners.dto;

public record ResetPasswordRequest(String email, String token, String novaSenha) {
}
