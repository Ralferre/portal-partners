package com.example.portalpartners.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(name = "arquivo_path")
    private String arquivoPath;

    @Column(name = "content_type")
    private String contentType;

    @Column(nullable = false)
    private LocalDateTime dataPostagem;

    @Column(name = "data_download_contratante")
    private LocalDateTime dataDownloadContratante;

    @Column(name = "data_status_atualizado")
    private LocalDateTime dataStatusAtualizado;

    @Enumerated(EnumType.STRING)
    private StatusDocumento statusDocumento;

    @PrePersist
    private void prePersist() {
        if (this.dataPostagem == null) {
            this.dataPostagem = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getArquivoPath() {
        return arquivoPath;
    }

    public void setArquivoPath(String arquivoPath) {
        this.arquivoPath = arquivoPath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public StatusDocumento getStatusDocumento() {
        return statusDocumento;
    }

    public void setStatusDocumento(StatusDocumento statusDocumento) {
        this.statusDocumento = statusDocumento;
    }

    public LocalDateTime getDataDownloadContratante() {
        return dataDownloadContratante;
    }

    public void setDataDownloadContratante(LocalDateTime dataDownloadContratante) {
        this.dataDownloadContratante = dataDownloadContratante;
    }

    public LocalDateTime getDataStatusAtualizado() {
        return dataStatusAtualizado;
    }

    public void setDataStatusAtualizado(LocalDateTime dataStatusAtualizado) {
        this.dataStatusAtualizado = dataStatusAtualizado;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    public Contratada getContratada() {
        return contratada;
    }

    public void setContratada(Contratada contratada) {
        this.contratada = contratada;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }
}


