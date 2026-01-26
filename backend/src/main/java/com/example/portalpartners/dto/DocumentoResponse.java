package com.example.portalpartners.dto;

import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
import jakarta.persistence.PrePersist;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentoResponse {
    private Long id;
    private TipoDocumento tipoDocumento;
    private String nomeArquivo;
    private StatusDocumento statusDocumento;
    private LocalDateTime dataPostagem;
    private String contratadaNome;
    private String funcionarioNome;

}
