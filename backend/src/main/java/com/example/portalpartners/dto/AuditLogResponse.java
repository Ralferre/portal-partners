package com.example.portalpartners.dto;

import com.example.portalpartners.model.AuditLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.portalpartners.model.StatusAuditoria;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AuditLogResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UUID id;
    private LocalDateTime timestamp;
    private Long userId;
    private String email;
    private String role;
    private Long organizacaoId;
    private String acao;
    private String entidade;
    private String entidadeId;
    private JsonNode detalhesJson;
    private String ip;
    private String userAgent;
    private StatusAuditoria status;
    private String mensagemErro;

    public static AuditLogResponse fromEntity(AuditLog entity) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setUserId(entity.getUserId());
        dto.setEmail(entity.getEmail());
        dto.setRole(entity.getRole());
        dto.setOrganizacaoId(entity.getOrganizacaoId());
        dto.setAcao(entity.getAcao());
        dto.setEntidade(entity.getEntidade());
        dto.setEntidadeId(entity.getEntidadeId());
        dto.setDetalhesJson(parseDetalhes(entity.getDetalhesJson()));
        dto.setIp(entity.getIp());
        dto.setUserAgent(entity.getUserAgent());
        dto.setStatus(entity.getStatus());
        dto.setMensagemErro(entity.getMensagemErro());
        return dto;
    }

    private static JsonNode parseDetalhes(String detalhesJson) {
        if (detalhesJson == null || detalhesJson.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(detalhesJson);
        } catch (Exception e) {
            return OBJECT_MAPPER.getNodeFactory().textNode(detalhesJson);
        }
    }
}
