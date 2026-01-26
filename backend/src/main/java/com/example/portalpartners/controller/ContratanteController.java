package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateContratanteRequest;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.repository.ContratanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contratantes")
@RequiredArgsConstructor
@Transactional
public class ContratanteController {
    private final ContratanteRepository contratanteRepository;

    @GetMapping
    public List<Contratante> contratanteList() {
        return contratanteRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Contratante> createContratante(@RequestBody CreateContratanteRequest createContratanteRequest) {
        if (contratanteRepository.existsByNome(createContratanteRequest.nome())) {
            return ResponseEntity.badRequest().build();
        }

        Contratante contratante = Contratante.builder()
                .email(createContratanteRequest.email())
                .nome(createContratanteRequest.nome())
                .senha(createContratanteRequest.senha())
                .build();
        return ResponseEntity.ok(contratanteRepository.save(contratante));
    }
}
