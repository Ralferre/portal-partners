package com.example.portalpartners.dto;

import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Funcionario;

public record FuncionarioResponse(
        Long id,
        String cpf,
        String nomeCompleto
//        Contratada contratada
) {
    public static FuncionarioResponse fromEntity(Funcionario f) {
        return new FuncionarioResponse(
                f.getId(),
                f.getCpf(),
                f.getNomeCompleto()
        );
    }
}
