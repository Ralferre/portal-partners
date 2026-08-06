package com.example.portalpartners.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Cria os indices definidos em db/migration que nunca sao aplicados.
 *
 * Contexto: os arquivos V2/V3/V4 foram escritos para execucao manual (o projeto
 * nao tem Flyway nem Liquibase no classpath). As COLUNAS ja sao garantidas pelo
 * Hibernate (ddl-auto=update) e pelo LegacySchemaCompatibilityInitializer, mas
 * os INDICES nao sao gerados por nenhum dos dois. O mais critico deles e o
 * indice unico de funcionario.cpf_hash: sem ele o banco aceita CPF duplicado.
 *
 * Roda depois do LegacySchemaCompatibilityInitializer (@Order 2 vs 1) porque o
 * indice unico so pode ser criado apos o backfill de cpf_hash.
 *
 * Todas as instrucoes sao idempotentes (IF NOT EXISTS) e cada falha e isolada:
 * um indice que nao pode ser criado gera WARN e nao impede a aplicacao de subir.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaIndexInitializer {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void ensureIndexes() {
        indexesAuditLog();
        indexesLgpdConsent();
        indexUnicoCpfHash();
    }

    private void indexesAuditLog() {
        if (!tabelaExiste("audit_log")) {
            return;
        }

        criarIndice("idx_audit_log_timestamp_user",
                "CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp_user ON audit_log (timestamp, user_id)");
        criarIndice("idx_audit_log_organizacao",
                "CREATE INDEX IF NOT EXISTS idx_audit_log_organizacao ON audit_log (organizacao_id, timestamp)");
        criarIndice("idx_audit_log_acao",
                "CREATE INDEX IF NOT EXISTS idx_audit_log_acao ON audit_log (acao)");
        criarIndice("idx_audit_log_status",
                "CREATE INDEX IF NOT EXISTS idx_audit_log_status ON audit_log (status)");

        // O indice GIN so e valido se a coluna tiver sido criada como jsonb.
        if ("jsonb".equalsIgnoreCase(tipoColuna("audit_log", "detalhes_json"))) {
            criarIndice("idx_audit_log_detalhes_gin",
                    "CREATE INDEX IF NOT EXISTS idx_audit_log_detalhes_gin ON audit_log USING GIN (detalhes_json)");
        } else {
            log.warn("audit_log.detalhes_json nao e jsonb; indice GIN nao criado. "
                    + "Queries JSON nativas na tela de auditoria farao full scan.");
        }
    }

    private void indexesLgpdConsent() {
        if (!tabelaExiste("lgpd_consent")) {
            return;
        }

        criarIndice("idx_lgpd_consent_user_versao",
                "CREATE INDEX IF NOT EXISTS idx_lgpd_consent_user_versao ON lgpd_consent (user_id, versao_termo)");
    }

    private void indexUnicoCpfHash() {
        if (!tabelaExiste("funcionario") || !colunaExiste("funcionario", "cpf_hash")) {
            return;
        }

        Integer duplicados = jdbcTemplate.queryForObject(
                "select count(*) from (select cpf_hash from funcionario "
                        + "where cpf_hash is not null group by cpf_hash having count(*) > 1) d",
                Integer.class);

        if (duplicados != null && duplicados > 0) {
            log.error("Existem {} valores de cpf_hash duplicados em funcionario. "
                    + "O indice unico NAO foi criado - o banco segue aceitando CPF repetido. "
                    + "Resolva as duplicidades e reinicie a aplicacao.", duplicados);
            return;
        }

        criarIndice("idx_funcionario_cpf_hash_unique",
                "CREATE UNIQUE INDEX IF NOT EXISTS idx_funcionario_cpf_hash_unique "
                        + "ON funcionario (cpf_hash) WHERE cpf_hash IS NOT NULL");
    }

    private void criarIndice(String nome, String ddl) {
        try {
            jdbcTemplate.execute(ddl);
            log.debug("Indice garantido: {}", nome);
        } catch (Exception e) {
            log.warn("Falha ao criar o indice {}: {}", nome, e.getMessage());
        }
    }

    private boolean tabelaExiste(String tabela) {
        Integer total = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables "
                        + "where table_schema = current_schema() and table_name = ?",
                Integer.class, tabela);
        return total != null && total > 0;
    }

    private boolean colunaExiste(String tabela, String coluna) {
        return tipoColuna(tabela, coluna) != null;
    }

    private String tipoColuna(String tabela, String coluna) {
        return jdbcTemplate.query(
                "select data_type from information_schema.columns "
                        + "where table_schema = current_schema() and table_name = ? and column_name = ?",
                rs -> rs.next() ? rs.getString("data_type") : null,
                tabela, coluna);
    }
}
