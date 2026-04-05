package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateContratadaRequest(
        @NotBlank String cnpj,
        @NotBlank String nome,
        @NotBlank String email,
        @Size(min = 8) String senha,
        @NotBlank String numeroContrato,
        @NotBlank String numeroPedido
) { }
