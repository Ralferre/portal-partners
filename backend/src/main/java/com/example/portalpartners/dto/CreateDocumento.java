package com.example.portalpartners.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class CreateDocumento {
    private Long contratadaId;
    private Long funcionarioId;
    private String tipoDocumento;
    private MultipartFile arquivo;
}
