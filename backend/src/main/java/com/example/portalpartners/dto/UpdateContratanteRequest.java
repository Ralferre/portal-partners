package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContratanteRequest(
        @NotBlank String nome,
        String cnpj,
        @NotBlank String email,
        @Size(min = 8) String senha
) { }
