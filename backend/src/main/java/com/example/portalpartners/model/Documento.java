package com.example.portalpartners.model;

import com.example.portalpartners.crypto.EncryptedStringConverter;
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

    /** Campo legado — mantido para retrocompatibilidade com uploads anteriores. */
    @Column(nullable = false)
    private String nomeArquivo;

    /**
     * Nome original do arquivo cifrado com AES-256-GCM.
     * Usado pela arquitetura Zero-Copy (novos uploads via presigned URL).
     * Nunca armazenado em plaintext para proteger dados pessoais identificaveis
     * (ex: "RG_joao_silva.pdf").
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "nome_arquivo_original")
    private String nomeArquivoOriginal;

    /** Path legado no MinIO (upload via backend). Mantido para retrocompat. */
    @Column(name = "arquivo_path")
    private String arquivoPath;

    /**
     * Object key opaco no MinIO para novos uploads (UUID v4).
     * Sem qualquer relacao semantica com o nome original do arquivo.
     * Um atacante com acesso ao banco ve apenas um UUID, sem informacao
     * de conteudo ou titular do documento.
     */
    @Column(name = "object_key")
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNomeArquivo() { return nomeArquivo; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }

    public String getNomeArquivoOriginal() { return nomeArquivoOriginal; }
    public void setNomeArquivoOriginal(String nomeArquivoOriginal) { this.nomeArquivoOriginal = nomeArquivoOriginal; }

    public String getArquivoPath() { return arquivoPath; }
    public void setArquivoPath(String arquivoPath) { this.arquivoPath = arquivoPath; }

    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }

    public StatusDocumento getStatusDocumento() { return statusDocumento; }
    public void setStatusDocumento(StatusDocumento statusDocumento) { this.statusDocumento = statusDocumento; }

    public LocalDateTime getDataDownloadContratante() { return dataDownloadContratante; }
    public void setDataDownloadContratante(LocalDateTime dataDownloadContratante) { this.dataDownloadContratante = dataDownloadContratante; }

    public LocalDateTime getDataStatusAtualizado() { return dataStatusAtualizado; }
    public void setDataStatusAtualizado(LocalDateTime dataStatusAtualizado) { this.dataStatusAtualizado = dataStatusAtualizado; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    public Contratada getContratada() { return contratada; }
    public void setContratada(Contratada contratada) { this.contratada = contratada; }

    public Funcionario getFuncionario() { return funcionario; }
    public void setFuncionario(Funcionario funcionario) { this.funcionario = funcionario; }
}
