package com.example.portalpartners.dto;

import java.time.LocalDateTime;

public record LgpdTermoAtualResponse(
        boolean valido,
        String versaoTermo,
        String hashTermo,
        String textoTermo,
        LocalDateTime timestampConsentimento
) {}
