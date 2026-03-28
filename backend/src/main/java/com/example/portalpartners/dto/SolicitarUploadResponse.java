package com.example.portalpartners.dto;

/**
 * Resposta da solicitacao de upload.
 * O frontend usa uploadUrl para fazer PUT diretamente no MinIO.
 * Apos o PUT, confirma o upload via POST /api/documentos/{documentoId}/confirmar-upload.
 */
public record SolicitarUploadResponse(
        Long documentoId,
        String objectKey,
        String uploadUrl,
        int expiresInSeconds
) {}
