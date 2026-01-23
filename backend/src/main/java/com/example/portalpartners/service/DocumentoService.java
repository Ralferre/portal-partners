package com.example.portalpartners.service;

import com.example.portalpartners.dto.CreateDocumento;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.TipoDocumento;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {
    private final DocumentoRepository documentoRepository;
    private final ContratadaRepository contratadaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final MinioService minioService;

    @Transactional
    public Documento uploadDocumento(CreateDocumento dto) {
        System.out.println("Teste endpoint: " + dto.getContratadaId());
        if (dto.getContratadaId() == null) {
            throw new IllegalArgumentException("ID da contratada é obrigatório");
        }

        TipoDocumento tipoEnum;
        try {
            tipoEnum = TipoDocumento.valueOf(dto.getTipoDocumento().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de documento inválido: " + dto.getTipoDocumento());
        }

        String path = minioService.uploadFile(
                dto.getArquivo(),
                dto.getContratadaId(),
                dto.getFuncionarioId(),
                TipoDocumento.valueOf(tipoEnum.name())
        );

        Documento documento = Documento.builder()
                .tipoDocumento(TipoDocumento.valueOf(dto.getTipoDocumento()))
                .nomeArquivo(dto.getArquivo().getOriginalFilename())
                .tipo(dto.getTipoDocumento())
                .status("PENDENTE")
                .dataPostagem(LocalDateTime.now())  // Captura automática
                .contratada(dto.getContratadaId() != null ? contratadaRepository.findById(dto.getContratadaId()).orElse(null) : null)
                .funcionario(dto.getFuncionarioId() != null ? funcionarioRepository.findById(dto.getFuncionarioId()).orElse(null) : null)
                .build();

        return documentoRepository.save(documento);
    }

    public List<Documento> findByContratadaUuid(Long contratadaId) {
        return documentoRepository.findByContratadaId(contratadaId);
    }

    public List<Documento> findByFuncionarioId(Long funcionarioId) {
        return documentoRepository.findByFuncionarioId(funcionarioId);
    }

    public List<Documento> findByContratadaIdAndTipo(Long contratadaId, String tipo) {
        return documentoRepository.findByContratadaIdAndTipo(contratadaId, tipo);
    }

}
