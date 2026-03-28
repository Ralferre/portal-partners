-- ============================================================
-- V4: Adicionar campos de seguranca na tabela documento
--
-- nome_arquivo_original: nome real do arquivo cifrado com AES-256-GCM
--   (preenchido pela arquitetura Zero-Copy / presigned URL)
-- object_key: UUID v4 opaco como chave no MinIO
--   (sem relacao semantica com o conteudo ou titular do documento)
--
-- Os campos legados nome_arquivo e arquivo_path sao MANTIDOS
-- para retrocompatibilidade com uploads anteriores.
-- ============================================================

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS nome_arquivo_original TEXT;

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS object_key VARCHAR(255);

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(255);

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS tamanho_bytes BIGINT;

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS data_download_contratante TIMESTAMP;

ALTER TABLE documento
    ADD COLUMN IF NOT EXISTS data_status_atualizado TIMESTAMP;

COMMENT ON COLUMN documento.nome_arquivo_original IS
    'Nome original do arquivo cifrado com AES-256-GCM. '
    'Decifrado transparentemente pelo EncryptedStringConverter JPA. '
    'Nunca armazenado em plaintext.';

COMMENT ON COLUMN documento.object_key IS
    'UUID v4 opaco como chave do objeto no MinIO. '
    'Sem relacao semantica com o nome real do arquivo ou titular. '
    'Usado pela arquitetura Zero-Copy (presigned URL).';

-- ============================================================
-- V4b: Adicionar campo cpf_hash na tabela funcionario
--
-- cpf_hash: HMAC-SHA256 do CPF normalizado (deterministico).
-- Permite queries de existencia/unicidade sem expor o CPF real.
-- A coluna cpf passa a armazenar o CPF cifrado com AES-256-GCM.
-- ============================================================

ALTER TABLE funcionario
    ADD COLUMN IF NOT EXISTS cpf_hash VARCHAR(255);

-- Remover unique constraint do cpf (agora cifrado, nao pode ser comparado diretamente)
ALTER TABLE funcionario
    DROP CONSTRAINT IF EXISTS funcionario_cpf_key;

CREATE UNIQUE INDEX IF NOT EXISTS idx_funcionario_cpf_hash_unique
    ON funcionario (cpf_hash)
    WHERE cpf_hash IS NOT NULL;

COMMENT ON COLUMN funcionario.cpf IS
    'CPF cifrado com AES-256-GCM. '
    'Para busca/unicidade usar cpf_hash (HMAC-SHA256 deterministico).';

COMMENT ON COLUMN funcionario.cpf_hash IS
    'HMAC-SHA256 do CPF normalizado. '
    'Deterministico: mesmo CPF sempre produz mesmo hash com a mesma chave. '
    'Permite queries de existencia sem expor o CPF em plaintext.';

-- ============================================================
-- NOTA: Migracao de dados existentes
--
-- Para registros legados (anteriores a esta migracao):
-- 1. cpf_hash pode estar NULL ate a primeira edicao/recreacao do funcionario.
--    O backend tambem faz backfill automatico desse campo na inicializacao.
-- 2. nome_arquivo_original ficara NULL para documentos antigos (usar nome_arquivo).
-- 3. object_key ficara NULL para documentos antigos (usar arquivo_path).
--
-- Script de backfill (executar em horario de baixo uso):
--   UPDATE funcionario SET cpf_hash = hmac(cpf, 'APP_FIELD_ENCRYPTION_KEY', 'sha256')
--   WHERE cpf_hash IS NULL;
--   (Requer extensao pgcrypto: CREATE EXTENSION IF NOT EXISTS pgcrypto;)
-- ============================================================
