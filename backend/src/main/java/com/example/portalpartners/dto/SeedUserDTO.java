package com.example.portalpartners.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeedUserDTO {
    private String email;
    private String senha;
    private String tipo;
}