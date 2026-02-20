package com.example.portalpartners.dto;

import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
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

    public static DocumentoResponse fromEntity(Documento documento) {
        DocumentoResponse response = new DocumentoResponse();

        response.setId(documento.getId());
        response.setTipoDocumento(documento.getTipoDocumento());
        response.setNomeArquivo(documento.getNomeArquivo());
        response.setStatusDocumento(documento.getStatusDocumento());
        response.setDataPostagem(documento.getDataPostagem());

        response.setContratadaNome(
                documento.getContratada() != null
                        ? documento.getContratada().getNome()
                        : null
        );

        response.setFuncionarioNome(
                documento.getFuncionario() != null
                        ? documento.getFuncionario().getNomeCompleto()
                        : null
        );

        return response;
    }
}
