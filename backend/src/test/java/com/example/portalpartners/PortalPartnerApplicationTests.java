package com.example.portalpartners;

import org.junit.jupiter.api.Test;

/**
 * Smoke test: garante que o contexto Spring sobe inteiro — datasource,
 * Flyway, JPA, seguranca, cliente S3 e beans de negocio — contra Postgres
 * e MinIO reais (Testcontainers). Se qualquer bean estiver mal configurado,
 * este teste falha.
 */
class PortalPartnerApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Sucesso = contexto subiu sem lancar excecao.
    }
}
