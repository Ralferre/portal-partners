-- ============================================================
-- V5: Estender tipos de documento com CND_FEDERAL/ESTADUAL/MUNICIPAL
--
-- O enum TipoDocumento e persistido como VARCHAR (@Enumerated(STRING)).
-- O schema tem um CHECK constraint que restringe os valores aceitos.
-- Esta migration recria esse CHECK incluindo os tres novos tipos de
-- Certidao Negativa de Debitos (federal, estadual, municipal), usados
-- pelos documentos de empresa.
--
-- O nome do constraint gerado pelo Hibernate e automatico e varia por
-- ambiente, entao localizamos e removemos qualquer CHECK existente sobre
-- a coluna documento.tipo_documento antes de criar o novo, nomeado.
-- Idempotente.
-- ============================================================

DO $$
DECLARE
    c record;
BEGIN
    -- Remove qualquer CHECK constraint atual que referencie tipo_documento.
    FOR c IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'documento'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) ILIKE '%tipo_documento%'
    LOOP
        EXECUTE format('ALTER TABLE documento DROP CONSTRAINT %I', c.conname);
    END LOOP;

    -- Cria o CHECK nomeado com a lista completa (idempotente).
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_documento_tipo_documento'
    ) THEN
        ALTER TABLE documento ADD CONSTRAINT chk_documento_tipo_documento
            CHECK (tipo_documento IN (
                'CNPJ','PGR','PCMSO','ORDEM_SERVICO','CNDT','CNAT','CNRF','GRIP',
                'CTPS_DIGITAL','INSS','FGTS','HOLERITE','ASO','RG','CPF','FICHA_EPI',
                'NR10','NR12','NR35',
                'CND_FEDERAL','CND_ESTADUAL','CND_MUNICIPAL'
            ));
    END IF;
END $$;
