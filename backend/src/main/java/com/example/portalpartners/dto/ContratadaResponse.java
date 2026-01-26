package com.example.portalpartners.dto;

public record ContratadaResponse(
        Long id,
        String cnpj,
        String nome,
        String email,
        String senha,
        String numeroPedido,
        String numeroContrato,
        Long contratanteId,
        String nomeContratante
) { }
