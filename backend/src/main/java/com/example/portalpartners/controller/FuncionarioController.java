package com.example.portalpartners.controller;

import com.example.portalpartners.dto.CreateFuncionarioRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Funcionario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
@RequiredArgsConstructor
public class FuncionarioController {
    private final FuncionarioRepository funcionarioRepository;
    private final ContratadaRepository contratadaRepository;

    @GetMapping
    public List<FuncionarioResponse> funcionarioList() {
        return funcionarioRepository.findAll()
                .stream()
                .map(f -> new FuncionarioResponse(
                        f.getId(),
                        f.getCpf(),
                        f.getNomeCompleto(),
                        f.getContratada().getId()
                ))
                .toList();
    }

    @PostMapping
    public ResponseEntity<FuncionarioResponse> createFuncionario(@RequestBody CreateFuncionarioRequest createFuncionarioRequest) {
        if (funcionarioRepository.existsByCpf(createFuncionarioRequest.cpf())) {
            return ResponseEntity.badRequest().build();
        }

        Contratada contratada = contratadaRepository.findById(createFuncionarioRequest.contratadaId())
                .orElseThrow(() -> new RuntimeException("Contratada não encontrada"));


        Funcionario funcionario = Funcionario.builder()
                .cpf(createFuncionarioRequest.cpf())
                .nomeCompleto(createFuncionarioRequest.nomeCompleto())
                .contratada(contratada)
                .build();
        funcionario = funcionarioRepository.save(funcionario);

        FuncionarioResponse response = new FuncionarioResponse(
                funcionario.getId(),
                funcionario.getCpf(),
                funcionario.getNomeCompleto(),
                funcionario.getContratada().getId()
        );

        return ResponseEntity.ok(response);

    }
}
