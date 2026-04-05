package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUsuarioContratanteRequest(
        @NotBlank String nome,
        @NotBlank String email,
        @NotBlank @Size(min = 8) String senha
) {
}
