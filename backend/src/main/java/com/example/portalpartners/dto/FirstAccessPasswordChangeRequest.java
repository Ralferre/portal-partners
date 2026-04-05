package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FirstAccessPasswordChangeRequest(
        @NotBlank
        String senhaAtual,

        @NotBlank
        @Size(min = 8)
        String novaSenha
) {
}
