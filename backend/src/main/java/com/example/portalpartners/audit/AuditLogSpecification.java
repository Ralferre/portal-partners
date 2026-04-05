package com.example.portalpartners.audit;

import com.example.portalpartners.model.AuditLog;
import com.example.portalpartners.model.StatusAuditoria;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuditLogSpecification {

    private AuditLogSpecification() {}

    public static Specification<AuditLog> comFiltros(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Long userId,
            String email,
            List<String> acoes,
            String entidade,
            StatusAuditoria status,
            Long organizacaoId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"));
            }
            if (acoes != null) {
                List<String> acoesFiltradas = acoes.stream()
                        .filter(acao -> acao != null && !acao.isBlank())
                        .map(String::trim)
                        .collect(Collectors.toList());
                if (!acoesFiltradas.isEmpty()) {
                    predicates.add(root.get("acao").in(acoesFiltradas));
                }
            }
            if (entidade != null && !entidade.isBlank()) {
                predicates.add(cb.equal(root.get("entidade"), entidade));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (organizacaoId != null) {
                predicates.add(cb.equal(root.get("organizacaoId"), organizacaoId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
