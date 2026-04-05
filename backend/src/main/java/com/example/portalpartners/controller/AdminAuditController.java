package com.example.portalpartners.controller;

import com.example.portalpartners.audit.AuditLogSpecification;
import com.example.portalpartners.dto.AuditLogResponse;
import com.example.portalpartners.model.AuditLog;
import com.example.portalpartners.model.StatusAuditoria;
import com.example.portalpartners.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminAuditController {

    private final AuditLogRepository auditLogRepository;
    private static final Set<String> SORT_FIELDS = Set.of(
            "timestamp", "userId", "email", "role", "organizacaoId", "acao", "entidade", "status"
    );

    /**
     * Lista registros do audit_log com filtros e paginacao.
     * Acessivel apenas pelo perfil ADMIN.
     *
     * Filtros suportados:
     *   startDate, endDate  - periodo (ISO date-time)
     *   userId              - ID do usuario
     *   email               - busca parcial por email
     *   acao                - acao exata (ex: LOGIN_SUCESSO)
     *   entidade            - tipo de entidade (ex: Documento)
     *   status              - SUCCESS ou FAILURE
     *   organizacaoId       - ID da organizacao
     *   page, size          - paginacao
     *   sortBy, sortDir     - ordenacao (padrao: timestamp DESC)
     */
    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> listar(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,

            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) String entidade,
            @RequestParam(required = false) StatusAuditoria status,
            @RequestParam(required = false) Long organizacaoId,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        String sortBySeguro = SORT_FIELDS.contains(sortBy) ? sortBy : "timestamp";
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBySeguro).ascending()
                : Sort.by(sortBySeguro).descending();

        List<String> acoes = acao == null || acao.isBlank()
                ? List.of()
                : Arrays.stream(acao.split(","))
                        .map(String::trim)
                        .filter(valor -> !valor.isBlank())
                        .toList();

        Specification<AuditLog> spec = AuditLogSpecification.comFiltros(
                startDate, endDate, userId, email, acoes, entidade, status, organizacaoId);

        Page<AuditLogResponse> resultado = auditLogRepository
                .findAll(spec, PageRequest.of(page, size, sort))
                .map(AuditLogResponse::fromEntity);

        return ResponseEntity.ok(resultado);
    }
}
