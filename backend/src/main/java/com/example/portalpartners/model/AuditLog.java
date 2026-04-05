package com.example.portalpartners.model;

import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_timestamp_user", columnList = "timestamp,user_id"),
        @Index(name = "idx_audit_log_organizacao",    columnList = "organizacao_id,timestamp"),
        @Index(name = "idx_audit_log_acao",            columnList = "acao"),
        @Index(name = "idx_audit_log_status",          columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 255)
    private String email;

    @Column(length = 50)
    private String role;

    @Column(name = "organizacao_id")
    private Long organizacaoId;

    @Column(nullable = false, length = 100)
    private String acao;

    @Column(length = 100)
    private String entidade;

    @Column(name = "entidade_id", length = 255)
    private String entidadeId;

    /**
     * Contexto adicional serializado como JSON.
     * Mapeado como JSONB no PostgreSQL para suportar queries JSON nativas.
     * capturarArgs=false por padrao na anotacao @Auditavel evita dados
     * sensiveis (senhas, CPF) de serem incluidos aqui.
     */
    @Type(JsonType.class)
    @Column(name = "detalhes_json", columnDefinition = "jsonb")
    private String detalhesJson;

    @Column(length = 50)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusAuditoria status;

    @Column(name = "mensagem_erro", columnDefinition = "TEXT")
    private String mensagemErro;
}
