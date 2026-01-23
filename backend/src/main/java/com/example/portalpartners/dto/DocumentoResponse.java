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
    private StatusDocumento status;
    private LocalDateTime dataPostagem;
    private Long contratadaId;
    private Long funcionarioId;

    @PrePersist
    private void prePersist() {
        if (this.dataPostagem == null) {
            this.dataPostagem = LocalDateTime.now();
        }
    }
}
