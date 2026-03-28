package com.example.portalpartners.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Registro de consentimento LGPD.
 * hashTermo: SHA-256 do texto integral do termo exibido ao usuario.
 * O backend valida esse hash contra o hash oficial configurado antes de persistir.
 */
public record LgpdConsentRequest(
        @NotBlank String versaoTermo,
        @NotBlank String hashTermo
) {}
