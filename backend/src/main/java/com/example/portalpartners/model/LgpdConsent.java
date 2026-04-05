package com.example.portalpartners.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "lgpd_consent",
    indexes = {
        @Index(name = "idx_lgpd_consent_user_versao", columnList = "user_id,versao_termo")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LgpdConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /**
     * Versao do texto do termo aceito (ex: "v1.0").
     * Permite evidenciar qual versao foi aceita em caso de auditoria juridica.
     */
    @Column(name = "versao_termo", nullable = false, length = 20)
    private String versaoTermo;

    @Column(length = 50)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * SHA-256 do texto integral do termo aceito.
     * Garante rastreabilidade do conteudo exato aceito pelo usuario,
     * independentemente de alteracoes futuras no texto.
     */
    @Column(name = "hash_termo", length = 255)
    private String hashTermo;
}
