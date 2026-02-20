package com.example.portalpartners.controller;

import com.example.portalpartners.documento.DocumentoSpecification;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final DocumentoRepository documentoRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalPostados", documentoRepository.count());
        report.put("totalAnalisados", documentoRepository.countByStatusDocumento(StatusDocumento.valueOf("ANALISADO")));
        report.put("totalAprovados", documentoRepository.countByStatusDocumento(StatusDocumento.valueOf("APROVADO")));
        report.put("totalReprovados", documentoRepository.countByStatusDocumento(StatusDocumento.valueOf("REPROVADO")));
        report.put("totalPendentes", documentoRepository.countByStatusDocumento(StatusDocumento.valueOf("PENDENTE")));
        return report;
    }

    @GetMapping("/documentos/ultimos-7-dias")
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    public List<DailyDocumentStats> ultimos7Dias() {
        Role role = usuarioLogadoService.getUsuario().getRole();

        Specification<Documento> base = Specification.where(null);
        if (role == Role.CONTRATADA) {
            base = base.and(
                    DocumentoSpecification.porContratadaId(
                            usuarioLogadoService.getContratadaLogada().getId()
                    )
            );
        } else if (role == Role.CONTRATANTE) {
            base = base.and(
                    DocumentoSpecification.porContratanteId(
                            usuarioLogadoService.getContratanteLogada().getId()
                    )
            );
        }

        LocalDate today = LocalDate.now();
        List<DailyDocumentStats> out = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();

            Specification<Documento> dayRange = (root, query, cb) ->
                    cb.and(
                            cb.greaterThanOrEqualTo(root.get("dataPostagem"), start),
                            cb.lessThan(root.get("dataPostagem"), end)
                    );

            long postados = documentoRepository.count(base.and(dayRange));
            long aprovados = documentoRepository.count(
                    base.and(dayRange).and((root, query, cb) -> cb.equal(root.get("statusDocumento"), StatusDocumento.APROVADO))
            );
            long pendentes = documentoRepository.count(
                    base.and(dayRange).and((root, query, cb) -> cb.equal(root.get("statusDocumento"), StatusDocumento.PENDENTE))
            );

            out.add(new DailyDocumentStats(day.toString(), postados, aprovados, pendentes));
        }

        return out;
    }

    public record DailyDocumentStats(String date, long postados, long aprovados, long pendentes) {}
}
