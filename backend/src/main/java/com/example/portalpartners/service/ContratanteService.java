package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.dto.ContratanteResponse;
import com.example.portalpartners.dto.CreateContratanteRequest;
import com.example.portalpartners.dto.UpdateContratanteRequest;
import com.example.portalpartners.exceptions.ConflictException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import com.example.portalpartners.util.EmailDomainUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContratanteService {
    private final ContratanteRepository contratanteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<ContratanteResponse> listarPaginado(int page) {
        return contratanteRepository.findAll(
                PageRequest.of(page, 10)
        ).map(ContratanteResponse::fromEntity);
    }

    @Auditavel(acao = "CONTRATANTE_CRIADA", entidade = "Contratante")
    public ContratanteResponse criar(CreateContratanteRequest request) {
        String email = request.email().trim().toLowerCase();
        String cnpj = normalizarCnpj(request.cnpj());
        String dominioEmail = EmailDomainUtils.extractDomain(email);

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("Email já está em uso");
        }

        if (cnpj == null || cnpj.isBlank()) {
            throw new ConflictException("CNPJ é obrigatório para criar nova organização contratante");
        }
        if (contratanteRepository.existsByCnpj(cnpj)) {
            throw new ConflictException("CNPJ já cadastrado para outra contratante");
        }
        if (contratanteRepository.existsByDominioEmail(dominioEmail)) {
            throw new ConflictException("Já existe uma contratante cadastrada com este domínio de e-mail");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.CONTRATANTE)
                .mustChangePassword(true)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        Contratante contratante = Contratante.builder()
                .nome(request.nome().trim())
                .cnpj(cnpj)
                .dominioEmail(dominioEmail)
                .usuario(usuarioSalvo)
                .build();

        Contratante saved = contratanteRepository.save(contratante);
        usuarioSalvo.setContratante(saved);
        usuarioRepository.save(usuarioSalvo);

        return ContratanteResponse.fromEntity(saved);
    }


    @Auditavel(acao = "CONTRATANTE_ATUALIZADA", entidade = "Contratante")
    @Transactional
    public ContratanteResponse atualizar(Long id, UpdateContratanteRequest request) {
        Contratante contratante = contratanteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contratante não encontrada"));

        Usuario usuario = contratante.getUsuario();
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuário da contratante não encontrado");
        }

        String novoEmail = request.email().trim().toLowerCase();
        if (!novoEmail.equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmail(novoEmail)) {
            throw new ConflictException("Email já está em uso");
        }

        String novoCnpj = normalizarCnpj(request.cnpj());
        if (novoCnpj != null && !novoCnpj.isBlank()) {
            if (!novoCnpj.equals(contratante.getCnpj()) && contratanteRepository.existsByCnpj(novoCnpj)) {
                throw new ConflictException("CNPJ já cadastrado para outra contratante");
            }
            contratante.setCnpj(novoCnpj);
        }

        String novoDominioEmail = EmailDomainUtils.extractDomain(novoEmail);
        if (!novoDominioEmail.equalsIgnoreCase(contratante.getDominioEmail())) {
            throw new ConflictException("Não é permitido alterar o domínio de e-mail da organização contratante");
        }

        contratante.setNome(request.nome().trim());
        usuario.setEmail(novoEmail);
        usuario.setNome(request.nome().trim());

        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        usuarioRepository.save(usuario);
        Contratante saved = contratanteRepository.save(contratante);
        return ContratanteResponse.fromEntity(saved);
    }

    public void buscarPorNome(String nome) {
        Contratante contratante = contratanteRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Contratante não encontrada"));

        contratanteRepository.findByNome(contratante.getNome());
    }
    
    @Auditavel(acao = "CONTRATANTE_EXCLUIDA", entidade = "Contratante")
    @Transactional
    public void removerPorNome(String nome) {
        Contratante contratante = contratanteRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Contratante não encontrada"));

        contratanteRepository.delete(contratante);
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        return cnpj.replaceAll("\\D", "");
    }
}
