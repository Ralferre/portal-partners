package com.example.portalpartners.config;

import com.example.portalpartners.crypto.FieldEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Garante compatibilidade com bases legadas que ainda nao receberam por completo
 * as colunas introduzidas na fase de auditoria/LGPD.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegacySchemaCompatibilityInitializer {

    private final JdbcTemplate jdbcTemplate;
    private final FieldEncryptionService fieldEncryptionService;

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void ensureCompatibility() {
        garantirColunasDocumento();
        garantirCpfHash();
        backfillCpfHash();
    }

    private void garantirColunasDocumento() {
        if (!tabelaExiste("documento")) {
            return;
        }

        garantirColuna("documento", "nome_arquivo_original", "TEXT");
        garantirColuna("documento", "object_key", "VARCHAR(255)");
        garantirColuna("documento", "content_type", "VARCHAR(255)");
        garantirColuna("documento", "tamanho_bytes", "BIGINT");
        garantirColuna("documento", "data_download_contratante", "TIMESTAMP");
        garantirColuna("documento", "data_status_atualizado", "TIMESTAMP");
    }

    private void garantirCpfHash() {
        if (!tabelaExiste("funcionario")) {
            return;
        }

        garantirColuna("funcionario", "cpf_hash", "VARCHAR(255)");
    }

    private void backfillCpfHash() {
        if (!tabelaExiste("funcionario") || !colunaExiste("funcionario", "cpf_hash")) {
            return;
        }

        List<FuncionarioLegadoRow> funcionarios = jdbcTemplate.query(
                "select id, cpf from funcionario where cpf_hash is null or trim(cpf_hash) = ''",
                (rs, rowNum) -> new FuncionarioLegadoRow(
                        rs.getLong("id"),
                        rs.getString("cpf")
                )
        );

        if (funcionarios.isEmpty()) {
            return;
        }

        int atualizados = 0;
        for (FuncionarioLegadoRow funcionario : funcionarios) {
            String cpfNormalizado = normalizarCpf(fieldEncryptionService.decrypt(funcionario.cpf()));
            if (cpfNormalizado == null || cpfNormalizado.isBlank()) {
                continue;
            }

            String cpfHash = fieldEncryptionService.hash(cpfNormalizado);
            int rows = jdbcTemplate.update(
                    "update funcionario set cpf_hash = ? where id = ? and (cpf_hash is null or trim(cpf_hash) = '')",
                    cpfHash,
                    funcionario.id()
            );
            atualizados += rows;
        }

        if (atualizados > 0) {
            log.info("Backfill de cpf_hash concluido para {} funcionario(s) legado(s)", atualizados);
        }
    }

    private void garantirColuna(String tabela, String coluna, String definicaoSql) {
        if (colunaExiste(tabela, coluna)) {
            return;
        }

        jdbcTemplate.execute(
                "ALTER TABLE " + tabela + " ADD COLUMN " + coluna + " " + definicaoSql
        );
        log.info("Coluna {}.{} criada para compatibilidade com base legada", tabela, coluna);
    }

    private boolean tabelaExiste(String tabela) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                select exists(
                    select 1
                    from information_schema.tables
                    where table_schema = current_schema()
                      and table_name = ?
                )
                """,
                Boolean.class,
                tabela
        );
        return Boolean.TRUE.equals(exists);
    }

    private boolean colunaExiste(String tabela, String coluna) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                select exists(
                    select 1
                    from information_schema.columns
                    where table_schema = current_schema()
                      and table_name = ?
                      and column_name = ?
                )
                """,
                Boolean.class,
                tabela,
                coluna
        );
        return Boolean.TRUE.equals(exists);
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        return cpf.replaceAll("\\D", "");
    }

    private record FuncionarioLegadoRow(Long id, String cpf) {}
}
