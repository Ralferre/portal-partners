package com.example.portalpartners.audit;

import com.example.portalpartners.model.StatusAuditoria;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AuditEventDTO {

    private String acao;
    private String entidade;
    private String entidadeId;

    private String email;
    private Long userId;
    private String role;
    private Long organizacaoId;

    private String ip;
    private String userAgent;

    private StatusAuditoria status;
    private String mensagemErro;

    /**
     * Contexto adicional livre. Nunca incluir senhas, tokens ou dados
     * pessoais sensiveis diretamente aqui — o conteudo e serializado
     * como JSONB no banco de dados.
     */
    private Map<String, Object> detalhes;
}
