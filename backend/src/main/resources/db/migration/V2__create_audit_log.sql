-- ============================================================
-- V2: Criacao da tabela audit_log
--
-- Complemento ao ddl-auto=update: garante o tipo JSONB e os
-- indices compostos que o Hibernate nao gera automaticamente.
-- Executar manualmente em ambiente de producao antes do deploy.
-- Em dev com volume limpo, o ddl-auto=update cria a tabela;
-- rodar este script apos para adicionar os indices.
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_log (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp     TIMESTAMP    NOT NULL,
    user_id       BIGINT,
    email         VARCHAR(255),
    role          VARCHAR(50),
    organizacao_id BIGINT,
    acao          VARCHAR(100) NOT NULL,
    entidade      VARCHAR(100),
    entidade_id   VARCHAR(255),
    detalhes_json JSONB,
    ip            VARCHAR(50),
    user_agent    VARCHAR(500),
    status        VARCHAR(20)  NOT NULL,
    mensagem_erro TEXT
);

-- Indices para suporte aos filtros da tela admin
CREATE INDEX IF NOT EXISTS idx_audit_log_timestamp_user
    ON audit_log (timestamp, user_id);

CREATE INDEX IF NOT EXISTS idx_audit_log_organizacao
    ON audit_log (organizacao_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_audit_log_acao
    ON audit_log (acao);

CREATE INDEX IF NOT EXISTS idx_audit_log_status
    ON audit_log (status);

-- Indice GIN para queries JSON nativas no campo JSONB
CREATE INDEX IF NOT EXISTS idx_audit_log_detalhes_gin
    ON audit_log USING GIN (detalhes_json);

COMMENT ON TABLE audit_log IS
    'Trilha de auditoria de todas as transacoes criticas do sistema. '
    'Registros persistidos de forma assincrona via @Async. '
    'Retencao: 365 dias (job diario de limpeza).';
