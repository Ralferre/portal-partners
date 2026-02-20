package com.example.portalpartners.dto;

import com.example.portalpartners.model.TipoDocumento;
import org.springframework.web.multipart.MultipartFile;

public record CreateDocumentoRequest(
        Long contratadaId,
        Long funcionarioId,
        TipoDocumento tipoDocumento,
        TypeReferenceFile tipoReferenciaDocumento,
        MultipartFile arquivo
) { }
