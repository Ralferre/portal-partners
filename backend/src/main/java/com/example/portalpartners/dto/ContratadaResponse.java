package com.example.portalpartners.dto;

import com.example.portalpartners.model.Contratada;

public record ContratadaResponse(
        Long id,
        String cnpj,
        String nome,
        String email,
        String numeroPedido,
        String numeroContrato,
        Long contratanteId,
        Long usuarioId
) {
    public static ContratadaResponse fromEntity(Contratada c) {
        String email = c.getUsuario() != null
                ? c.getUsuario().getEmail()
                : (c.getUsuarios() != null && !c.getUsuarios().isEmpty() ? c.getUsuarios().get(0).getEmail() : null);

        Long usuarioId = c.getUsuario() != null
                ? c.getUsuario().getId()
                : (c.getUsuarios() != null && !c.getUsuarios().isEmpty() ? c.getUsuarios().get(0).getId() : null);

        return new ContratadaResponse(
                c.getId(),
                c.getCnpj(),
                c.getNome(),
                email,
                c.getNumeroPedido(),
                c.getNumeroContrato(),
                c.getContratante().getId(),
                usuarioId
        );
    }
}
