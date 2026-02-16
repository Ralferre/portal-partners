package com.example.portalpartners.dto;

import com.example.portalpartners.model.Contratada;

public record ContratadaResponse(
        Long id,
        String cnpj,
        String nome,
        String numeroPedido,
        String numeroContrato,
        Long contratanteId,
        Long usuarioId
) {
    public static ContratadaResponse fromEntity(Contratada c) {
        return new ContratadaResponse(
                c.getId(),
                c.getCnpj(),
                c.getNome(),
                c.getNumeroPedido(),
                c.getNumeroContrato(),
                c.getContratante().getId(),
                c.getUsuario().getId()
        );
    }
}
