package com.example.portalpartners.model;

import com.example.portalpartners.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Documento enviado por uma CONTRATADA, vinculado a ela e/ou a um funcionario.
 *
 * Usa {@code @Getter}/{@code @Setter} (e nao {@code @Data}) de proposito:
 * entidades JPA com relacionamentos LAZY nao devem ter equals/hashCode/toString
 * gerados por {@code @Data}, pois disparam carregamento preguicoso e podem
 * recursar entre entidades relacionadas.
 */
@Entity
@Getter
@Setter
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @PrePersist
    private void prePersist() {
        if (this.dataPostagem == null) {
            this.dataPostagem = LocalDateTime.now();
        }
    }
}
