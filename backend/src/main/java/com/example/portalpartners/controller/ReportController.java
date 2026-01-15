package com.example.portalpartners.controller;

import com.example.portalpartners.repository.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {
    private final DocumentoRepository documentoRepository;

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> report = new HashMap<>();
        report.put("totalPostados", documentoRepository.count());
        report.put("totalAnalisados", documentoRepository.countByStatus("ANALISADO"));
        report.put("totalAprovados", documentoRepository.countByStatus("APROVADO"));
        report.put("totalReprovados", documentoRepository.countByStatus("REPROVADO"));
        return report;
    }
}
