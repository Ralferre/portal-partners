package com.example.portalpartners.dto;

import lombok.Data;

@Data
public class UsuarioDTO {
    private Long id;
    private String nome;
    private String email;
    private Boolean mustChangePassword;
    private Boolean principal;
}