package com.example.portalpartners.dto;

public record CreateFuncionarioRequest(
        String cpf,
        String nomeCompleto,
        Long contratadaId,
        String contratadaNome
) { }
