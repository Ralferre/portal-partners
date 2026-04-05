package com.example.portalpartners.audit;

import com.example.portalpartners.model.AuditLog;
import com.example.portalpartners.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persiste um evento de auditoria de forma assincrona no executor dedicado.
     *
     * O metodo e @Async: a thread da requisicao original nao espera
     * pela persistencia, garantindo que latencia de DB nao impacte o usuario.
     *
     * Excecoes de persistencia sao capturadas internamente: uma falha de
     * auditoria NUNCA deve derrubar a requisicao principal.
     */
    @Async("auditExecutor")
    public void registrar(AuditEventDTO evento) {
        try {
            String detalhesJson = null;
            if (evento.getDetalhes() != null && !evento.getDetalhes().isEmpty()) {
                detalhesJson = objectMapper.writeValueAsString(evento.getDetalhes());
            }

            AuditLog log = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .userId(evento.getUserId())
                    .email(evento.getEmail())
                    .role(evento.getRole())
                    .organizacaoId(evento.getOrganizacaoId())
                    .acao(evento.getAcao())
                    .entidade(evento.getEntidade())
                    .entidadeId(evento.getEntidadeId())
                    .detalhesJson(detalhesJson)
                    .ip(evento.getIp())
                    .userAgent(evento.getUserAgent())
                    .status(evento.getStatus())
                    .mensagemErro(evento.getMensagemErro())
                    .build();

            auditLogRepository.save(log);

        } catch (Exception e) {
            // Log local sem propagar: auditoria nunca interrompe o fluxo principal
            log.error("Falha ao persistir evento de auditoria [acao={}]: {}",
                    evento.getAcao(), e.getMessage(), e);
        }
    }

    /**
     * Job diario de retencao: remove registros com mais de 365 dias.
     * Executa uma vez por dia a 02:00 para minimizar impacto em producao.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void limparLogsAntigos() {
        try {
            LocalDateTime limite = LocalDateTime.now().minusDays(365);
            auditLogRepository.deleteByTimestampBefore(limite);
            log.info("Job de retencao de audit_log executado. Limite: {}", limite);
        } catch (Exception e) {
            log.error("Falha no job de retencao do audit_log: {}", e.getMessage(), e);
        }
    }
}
