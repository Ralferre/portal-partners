package com.example.portalpartners.dto;

public record FuncionarioResponse(
        Long id,
        String cpf,
        String nomeCompleto,
        Long contratadaId
) {
}
