package com.example.portalpartners.dto;

public record CreateContratanteRequest(
        String email,
        String nome,
        String senha,
        String telefone,
        String endereco

) { }
