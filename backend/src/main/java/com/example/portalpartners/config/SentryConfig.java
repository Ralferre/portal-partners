package com.example.portalpartners.config;

import io.sentry.SentryOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.regex.Pattern;

/**
 * Configuracao do Sentry com remocao de dados pessoais (PII) antes do envio.
 *
 * Garante que CPF, e-mail, tokens e cabecalhos de autenticacao nunca saiam da
 * aplicacao para o Sentry, mesmo que apareçam em mensagens de excecao. Alinhado
 * a LGPD e a politica de nao vazar dados sensiveis para terceiros.
 *
 * So e ativado quando ha um DSN configurado (variavel SENTRY_DSN).
 */
@Configuration
@ConditionalOnProperty(name = "sentry.dsn")
public class SentryConfig {

    // CPF (com ou sem mascara) e e-mails.
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._-]+");

    @Bean
    public SentryOptions.BeforeSendCallback scrubPii() {
        return (event, hint) -> {
            // Nunca enviar o usuario (pode conter email/ip).
            event.setUser(null);

            if (event.getRequest() != null) {
                event.getRequest().setCookies(null);
                event.getRequest().setHeaders(null);
            }

            if (event.getMessage() != null && event.getMessage().getFormatted() != null) {
                event.getMessage().setFormatted(mask(event.getMessage().getFormatted()));
            }
            return event;
        };
    }

    static String mask(String input) {
        if (input == null) return null;
        String out = CPF.matcher(input).replaceAll("[CPF_REMOVIDO]");
        out = EMAIL.matcher(out).replaceAll("[EMAIL_REMOVIDO]");
        out = BEARER.matcher(out).replaceAll("Bearer [TOKEN_REMOVIDO]");
        return out;
    }
}
