package com.example.portalpartners.controller;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadadRequest;

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
    public List<ContratadaResponse> contratadaList() {
        return contratadaRepository.findAll()
                .stream()
                .map(c -> new ContratadaResponse(
                        c.getId(),
                        c.getCnpj(),
                        c.getRazaoSocial(),
                        c.getNomeFantasia(),
                        c.getEmail()
//                        c.getContratante().getId(),
//                        c.getContratante().getNome()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<Contratada> createContratada(@RequestBody CreateContratadadRequest createContratadaRequest) {
        if (contratadaRepository.existsByCnpj(createContratadaRequest.cnpj())) {
            return ResponseEntity.badRequest().build();
        }

        Contratante contratante = contratanteRepository.findById(createContratadaRequest.contratanteId())
                .orElseThrow(() -> new RuntimeException("Contratante não encontrada"));

        Contratada contratada = Contratada.builder()
                .cnpj(createContratadaRequest.cnpj())
                .razaoSocial(createContratadaRequest.razaoSocial())
                .nomeFantasia(createContratadaRequest.nomeFantasia())
                .endereco(createContratadaRequest.endereco())
                .telefone(createContratadaRequest.telefone())
                .email(createContratadaRequest.email())
                .numeroContrato(createContratadaRequest.numeroContrato())
                .numeroPedido(createContratadaRequest.numeroPedido())
                .contratante(contratante)
                .build();
        return ResponseEntity.ok(contratadaRepository.save(contratada));
    }
}
