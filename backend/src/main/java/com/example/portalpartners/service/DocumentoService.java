package com.example.portalpartners.service;

import com.example.portalpartners.dto.CreateDocumentoRequest;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentoService {
    private final DocumentoRepository documentoRepository;
    private final ContratadaRepository contratadaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MinioService minioService;

    @Transactional
    public Documento uploadDocumento(CreateDocumentoRequest dto) {
        Funcionario funcionario = funcionarioRepository
                .findByNomeCompletoIgnoreCase(dto.funcionarioNome())
                .orElseThrow(() -> new IllegalArgumentException("Funcionário não encontrado"));

        Contratada contratada = contratadaRepository
                .findByNomeIgnoreCase(dto.contratadaNome())
                .orElseThrow(() -> new IllegalArgumentException("Contratada não encontrada"));

        TipoDocumento tipoEnum;
        try {
            tipoEnum = TipoDocumento.valueOf(dto.tipoDocumento().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de documento inválido: " + dto.tipoDocumento());
        }

        String path = minioService.uploadFile(
                dto.arquivo(),
                dto.contratadaNome(),
                dto.funcionarioNome(),
                TipoDocumento.valueOf(tipoEnum.name())
        );

        Documento documento = Documento.builder()
                .tipoDocumento(TipoDocumento.valueOf(dto.tipoDocumento()))
                .nomeArquivo(dto.arquivo().getOriginalFilename())
                .statusDocumento(StatusDocumento.valueOf("PENDENTE"))
                .dataPostagem(LocalDateTime.now())
                .contratada(dto.contratadaNome() != null ? contratadaRepository.findByNome(dto.contratadaNome()).orElse(null) : null)
                .funcionario(dto.funcionarioNome() != null ? funcionarioRepository.findByNomeCompleto(dto.funcionarioNome()).orElse(null) : null)
                .build();

        return documentoRepository.save(documento);
    }

    public List<Documento> findByContratadaNome(String contratadaNome) {
        return documentoRepository.findByContratadaNome(contratadaNome);
    }

    public List<Documento> findByFuncionarioId(Long funcionarioId) {
        return documentoRepository.findByFuncionarioId(funcionarioId);
    }

    public List<Documento> findByContratadaNomeAndTipo(String contratadaNome, TipoDocumento tipoDocumento) {
        return documentoRepository.findByContratadaNomeAndTipoDocumento(contratadaNome, tipoDocumento);
    }

    public Page<Documento> findByFuncionarioNomeContainingIgnoreCase(String nome, Pageable pageable) {
        return documentoRepository.findByFuncionarioNomeCompletoContainingIgnoreCase(nome, pageable);
    }

}
