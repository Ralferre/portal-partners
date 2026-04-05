package com.example.portalpartners.dto;

/**
 * Resposta da solicitacao de download.
 * O frontend usa downloadUrl para fazer GET diretamente no MinIO.
 * Os bytes do arquivo NUNCA passam pelo backend Java.
 */
public record SolicitarDownloadResponse(
        String downloadUrl,
        String nomeArquivo,
        String contentType,
        int expiresInSeconds
) {}
