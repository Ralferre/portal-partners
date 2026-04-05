package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContratanteRequest(
        String email,
        String nome,
        String cnpj,
        @NotBlank
        @Size(min = 8)
        String senha

) { }
