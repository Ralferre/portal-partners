package com.example.portalpartners.dto;

public record CreateContratadaRequest(
        String cnpj,
        String razaoSocial,
        String nomeFantasia,
        String endereco,
        String telefone,
        String email,
        String numeroContrato,
        String numeroPedido,
        Long contratanteId
) {
}
