package com.example.portalpartners.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitarios do scrubbing de PII enviado ao Sentry. Nao sobe contexto.
 */
class SentryConfigTest {

    @Test
    void mask_removeCpf() {
        assertThat(SentryConfig.mask("Erro ao validar CPF 123.456.789-09 do usuario"))
                .doesNotContain("123.456.789-09")
                .contains("[CPF_REMOVIDO]");
    }

    @Test
    void mask_removeCpfSemPontuacao() {
        assertThat(SentryConfig.mask("cpf 12345678909 invalido"))
                .doesNotContain("12345678909")
                .contains("[CPF_REMOVIDO]");
    }

    @Test
    void mask_removeEmail() {
        assertThat(SentryConfig.mask("Falha para joao.silva@empresa.com.br"))
                .doesNotContain("joao.silva@empresa.com.br")
                .contains("[EMAIL_REMOVIDO]");
    }

    @Test
    void mask_removeBearerToken() {
        assertThat(SentryConfig.mask("Authorization: Bearer eyJhbGciOi.J9.abc-123_XYZ"))
                .doesNotContain("eyJhbGciOi.J9.abc-123_XYZ")
                .contains("[TOKEN_REMOVIDO]");
    }

    @Test
    void mask_textoSemPii_permaneceIgual() {
        String limpo = "Falha ao conectar no storage apos 3 tentativas";
        assertThat(SentryConfig.mask(limpo)).isEqualTo(limpo);
    }
}
