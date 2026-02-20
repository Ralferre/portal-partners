package com.example.portalpartners.dto;

import com.example.portalpartners.model.Contratante;

public record ContratanteResponse(
        Long id,
        String nome,
        String email
) {
    public static ContratanteResponse fromEntity(Contratante c) {
        return new ContratanteResponse(
                c.getId(),
                c.getNome(),
                c.getUsuario().getEmail()
        );
    }
}
