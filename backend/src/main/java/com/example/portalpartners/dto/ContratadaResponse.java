package com.example.portalpartners.dto;

public record ContratadaResponse(
        Long id,
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String email,
        Long contratanteId,
        String nomeContratante
) {
}
