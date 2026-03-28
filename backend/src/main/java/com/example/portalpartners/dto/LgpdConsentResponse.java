package com.example.portalpartners.dto;

import java.time.LocalDateTime;

public record LgpdConsentResponse(
        boolean valido,
        String versaoTermo,
        LocalDateTime timestamp
) {}
