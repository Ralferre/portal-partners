package com.example.portalpartners.dto;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String senha;
}