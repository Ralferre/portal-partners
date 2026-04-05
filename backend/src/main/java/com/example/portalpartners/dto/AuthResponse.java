package com.example.portalpartners.dto;

import com.example.portalpartners.model.Role;


public record AuthResponse (
    String token,
    String nome,
    String email,
    Role role,
    Long perfilId,
    Boolean mustChangePassword
) {}