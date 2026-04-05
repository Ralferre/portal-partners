package com.example.portalpartners.dto;

import com.example.portalpartners.model.TipoDocumento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitacao de URL assinada para upload direto ao MinIO (Zero-Copy).
 * O backend valida permissoes, LGPD e gera a presigned PUT URL.
 * Os bytes do arquivo NUNCA passam pelo backend.
 */
public record SolicitarUploadRequest(

        @NotBlank
        String nomeArquivo,

        @NotBlank
        String contentType,

        @NotNull
        Long tamanhoBytes,

        @NotNull
        TipoDocumento tipoDocumento,

        @NotNull
        TypeReferenceFile tipoReferencia,

        Long funcionarioId,

        Long contratadaId
) {}
