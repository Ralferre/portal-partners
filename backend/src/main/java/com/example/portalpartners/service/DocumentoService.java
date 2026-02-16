package com.example.portalpartners.service;

import com.example.portalpartners.documento.DocumentoSpecification;
import com.example.portalpartners.dto.CreateDocumentoRequest;
import com.example.portalpartners.dto.DocumentoResponse;
import com.example.portalpartners.dto.TypeReferenceFile;
import com.example.portalpartners.exceptions.BusinessRulersException;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UsuarioLogadoService usuarioLogadoService;

    @Transactional
    public Documento uploadDocumento(CreateDocumentoRequest dto) {
        Usuario usuario = usuarioLogadoService.getUsuario();
        if (usuario.getRole() == Role.ADMIN || usuario.getRole() == Role.CONTRATANTE) {
            throw new BusinessRulersException("Usuário sem permissão");
        }

        Documento documento = new Documento();
        documento.setTipoDocumento(dto.tipoDocumento());
        documento.setStatusDocumento(StatusDocumento.PENDENTE);
        documento.setDataPostagem(LocalDateTime.now());

        if (dto.tipoReferenciaDocumento() == TypeReferenceFile.CONTRATADA) {

            if (dto.contratadaId() == null) {
                throw new BusinessRulersException("Contratada deve ser informada.");
            }

            Contratada contratada = contratadaRepository.findById(dto.contratadaId())
                    .orElseThrow(() -> new BusinessRulersException("Contratada não encontrada"));

            documento.setContratada(contratada);
            documento.setFuncionario(null);
        } else if (dto.tipoReferenciaDocumento() == TypeReferenceFile.FUNCIONARIO) {
            if (dto.funcionarioId() == null) {
                throw new BusinessRulersException("Funcionário deve ser informado");
            }

            Funcionario funcionario = funcionarioRepository.findById(dto.funcionarioId())
                    .orElseThrow(() -> new BusinessRulersException("Funcionário não encontrado"));

            documento.setFuncionario(funcionario);
            documento.setContratada(funcionario.getContratada());
        } else {
            throw new BusinessRulersException("Tipo de referência inválido");
        }

        documento.setNomeArquivo(dto.arquivo().getOriginalFilename());

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

    public Page<Documento> findAll(int page, int size) {
        return documentoRepository.findAll(PageRequest.of(page, size));
    }

    private Specification<Documento> restricaoPorPerfil(Usuario usuario) {

        if (usuario.getRole() == Role.ADMIN) {
            return Specification.where(null);
        }

        if (usuario.getRole() == Role.CONTRATADA) {
            return DocumentoSpecification.porContratadaId(
                    usuario.getId()
            );
        }

        if (usuario.getRole() == Role.CONTRATANTE) {
            return DocumentoSpecification.porContratanteId(
                    usuario.getId()
            );
        }

        throw new AccessDeniedException("Perfil sem permissão");
    }

    public Page<DocumentoResponse> filtrar(
            String contratadaNome,
            String funcionarioNome,
            TipoDocumento tipo,
            StatusDocumento status,
            Pageable pageable) {

        Usuario usuario = usuarioLogadoService.getUsuario();

        Specification<Documento> spec = Specification.where(null);

        // 🔐 filtro por perfil
        spec = spec.and(restricaoPorPerfil(usuario));

        // 🔎 filtros opcionais
        if (contratadaNome != null && !contratadaNome.isBlank()) {
            spec = spec.and(DocumentoSpecification.contratadaNomeLike(contratadaNome));
        }

        if (funcionarioNome != null && !funcionarioNome.isBlank()) {
            spec = spec.and(DocumentoSpecification.funcionarioNomeLike(funcionarioNome));
        }

        if (tipo != null) {
            spec = spec.and(DocumentoSpecification.tipoEquals(tipo));
        }

        if (status != null) {
            spec = spec.and(DocumentoSpecification.statusEquals(status));
        }

        return documentoRepository.findAll(spec, pageable);
    }

}
