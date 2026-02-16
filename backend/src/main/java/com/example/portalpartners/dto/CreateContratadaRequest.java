package com.example.portalpartners.dto;

public record CreateContratadaRequest(
        String cnpj,
        String nome,
        String email,
        String senha,
        String numeroContrato,
        String numeroPedido
) { }
