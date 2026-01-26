package com.example.portalpartners.controller;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;

import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.ContratanteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/contratadas")
@RequiredArgsConstructor
@Transactional
public class ContratadaController {
    private final ContratadaRepository contratadaRepository;
    private final ContratanteRepository contratanteRepository;

    @GetMapping
    public List<ContratadaResponse> contratadaList(Contratante contratante) {
        return contratadaRepository.findAll()
                .stream()
                .map(c -> new ContratadaResponse(
                        c.getId(),
                        c.getCnpj(),
                        c.getEmail(),
                        c.getNome(),
                        c.getSenha(),
                        c.getNumeroContrato(),
                        c.getNumeroPedido(),
                        contratante.getId(),
                        contratante.getNome()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<Contratada> createContratada(@RequestBody CreateContratadaRequest createContratadaRequest) {
        if (contratadaRepository.existsByCnpj(createContratadaRequest.cnpj())) {
            return ResponseEntity.badRequest().build();
        }

        Contratante contratante = contratanteRepository.findById(createContratadaRequest.contratanteId())
                .orElseThrow(() -> new RuntimeException("Contratante não encontrada"));

        if (createContratadaRequest.contratanteNome() != null &&
                !contratante.getNome().equalsIgnoreCase(createContratadaRequest.nome())) {
            throw new IllegalArgumentException("Nome da contratante não corresponde ao ID informado");
        }

        Contratada contratada = Contratada.builder()
                .cnpj(createContratadaRequest.cnpj())
                .nome(createContratadaRequest.nome())
                .email(createContratadaRequest.email())
                .senha(createContratadaRequest.senha())
                .numeroContrato(createContratadaRequest.numeroContrato())
                .numeroPedido(createContratadaRequest.numeroPedido())
                .contratante(contratante)
                .build();
        return ResponseEntity.ok(contratadaRepository.save(contratada));
    }
}
