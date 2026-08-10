package com.example.portalpartners.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Autorizacao de uso unico para baixar um documento.
 *
 * Substitui a presigned URL de 15 minutos no fluxo de download: o link deixa
 * de ser reaproveitavel. Uma vez consumido, o mesmo endereco nao serve mais
 * para ninguem, mesmo dentro da janela de validade.
 *
 * O proprio token e a credencial (256 bits de entropia), o que permite que o
 * link funcione em navegacao direta do browser, sem cabecalho Authorization.
 *
 * Nao guarda nome de arquivo: `nomeArquivoOriginal` e cifrado em repouso no
 * Documento e copiar o valor para ca em texto claro anularia essa protecao.
 * O nome e resolvido a partir do Documento no momento do download.
 */
@Entity
@Table(
        name = "download_token",
        indexes = {
                @Index(name = "idx_download_token_token", columnList = "token", unique = true),
                @Index(name = "idx_download_token_expira", columnList = "expira_em")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DownloadToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "documento_id", nullable = false)
    private Long documentoId;

    /** Usuario que solicitou o download. Serve para auditoria. */
    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    /** Chave opaca do objeto no storage, resolvida no momento da solicitacao. */
    @Column(name = "object_key", nullable = false)
    private String objectKey;

    /**
     * Indica que este download deve marcar `dataDownloadContratante` quando
     * consumido. Calculado na emissao, evitando recarregar o usuario depois.
     */
    @Column(name = "marca_download_contratante", nullable = false)
    private boolean marcaDownloadContratante;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    /** Nulo enquanto nao usado. Preenchido torna o token inutilizavel. */
    @Column(name = "consumido_em")
    private LocalDateTime consumidoEm;

    @Column(name = "ip_consumo", length = 50)
    private String ipConsumo;
}
