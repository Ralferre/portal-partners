-- ============================================================
-- V3: Criacao da tabela lgpd_consent
--
-- Registra cada aceite LGPD com versao e hash do termo.
-- hash_termo: SHA-256 do texto exibido ao usuario, permitindo
-- provar em auditoria qual conteudo exato foi aceito.
-- ============================================================

CREATE TABLE IF NOT EXISTS lgpd_consent (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     BIGINT      NOT NULL,
    timestamp   TIMESTAMP   NOT NULL,
    versao_termo VARCHAR(20) NOT NULL,
    ip          VARCHAR(50),
    user_agent  VARCHAR(500),
    hash_termo  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_lgpd_consent_user_versao
    ON lgpd_consent (user_id, versao_termo);

COMMENT ON TABLE lgpd_consent IS
    'Registros de consentimento LGPD por usuario e versao do termo. '
    'hash_termo permite evidenciar qual texto foi aceito em cada data. '
    'Manter registros historicos para fins juridicos.';

COMMENT ON COLUMN lgpd_consent.hash_termo IS
    'SHA-256 do texto integral do termo exibido ao usuario no momento do aceite.';
