package com.example.portalpartners.dto;

import org.springframework.web.multipart.MultipartFile;

public record CreateDocumentoRequest(
        String contratadaNome,
        String funcionarioNome,
        String tipoDocumento,
        MultipartFile arquivo
) { }
