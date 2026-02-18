package com.example.portalpartners.service;

import com.example.portalpartners.dto.CreateFuncionarioRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.exceptions.ConflictException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Funcionario;
import com.example.portalpartners.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
public class FuncionarioService {
    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    @Transactional
    public FuncionarioResponse criar(CreateFuncionarioRequest request) {

        Contratada contratada = usuarioLogadoService.getContratadaLogada();

        if (funcionarioRepository.existsByCpf(request.cpf())) {
            throw new ConflictException("Funcionário com este CPF já cadastrado");
        }

        Funcionario funcionario = Funcionario.builder()
                .nomeCompleto(request.nomeCompleto())
                .cpf(request.cpf())
                .contratada(contratada)
                .build();

        funcionarioRepository.save(funcionario);

        return FuncionarioResponse.fromEntity(funcionario);
    }

    public List<FuncionarioResponse> listar() {
        Contratada contratada = usuarioLogadoService.getContratadaLogada();

        return funcionarioRepository.findByContratada(contratada);
    }

    public List<FuncionarioResponse> findByContratada(String nome) {
        Contratada contratada = usuarioLogadoService.getContratadaLogada();

        return funcionarioRepository.findByContratada(contratada);
    }

    public FuncionarioResponse buscarFuncionarioPorNomeCompleto(String nomeCompleto) {
        Contratada contratada = usuarioLogadoService.getContratadaLogada();

        Funcionario funcionario = funcionarioRepository
                .findByContratadaAndNomeCompletoContainingIgnoreCase(contratada, nomeCompleto);

        if (funcionario == null) {
            throw new ResourceNotFoundException("Funcionário não encontrado");
        }
    return FuncionarioResponse.fromEntity(funcionario);
    }
}
