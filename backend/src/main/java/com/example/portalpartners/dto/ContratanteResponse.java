package com.example.portalpartners.dto;

import com.example.portalpartners.model.Contratante;

public record ContratanteResponse(
        Long id,
        String nome,
        String cnpj,
        String email
) {
    public static ContratanteResponse fromEntity(Contratante c) {
        String email = c.getUsuario() != null
                ? c.getUsuario().getEmail()
                : (c.getUsuarios() != null && !c.getUsuarios().isEmpty() ? c.getUsuarios().get(0).getEmail() : null);
        return new ContratanteResponse(
                c.getId(),
                c.getNome(),
                c.getCnpj(),
                email
        );
    }
}
