package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContratadaRequest(
        String cnpj,
        String nome,
        String email,
        @NotBlank
        @Size(min = 8)
        String senha,
        String numeroContrato,
        String numeroPedido
) { }
