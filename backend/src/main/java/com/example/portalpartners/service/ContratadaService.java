package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.dto.CreateContratadaRequest;
import com.example.portalpartners.dto.UpdateContratadaRequest;
import com.example.portalpartners.exceptions.ConflictException;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import com.example.portalpartners.util.EmailDomainUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContratadaService {
    private final ContratadaRepository contratadaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioLogadoService usuarioLogadoService;

    public Page<ContratadaResponse> listarPaginado(int page) {
        Contratante contratante = usuarioLogadoService.getContratanteLogada();
        return contratadaRepository
                .findByContratante(
                        contratante,
                        PageRequest.of(page, 10)
                )
                .map(ContratadaResponse::fromEntity);
    }

    public Page<ContratadaResponse> listarPorContratante(int page) {

        Contratante contratante = usuarioLogadoService.getContratanteLogada();

        return contratadaRepository
                .findByContratante(
                        contratante,
                        PageRequest.of(page, 10)
                )
                .map(ContratadaResponse::fromEntity);
    }

    public ContratadaResponse buscarPorNome(String nome) {
        Contratante contratante = usuarioLogadoService.getContratanteLogada();
        return contratadaRepository
                .findByContratanteAndNomeContainingIgnoreCase(contratante, nome)
                .map(ContratadaResponse::fromEntity)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Contratada não encontrada"));
    }

    @Auditavel(acao = "CONTRATADA_CRIADA", entidade = "Contratada")
    @Transactional
    public ContratadaResponse criar(CreateContratadaRequest request) {

        Contratante contratante = usuarioLogadoService.getContratanteLogada();
        String email = request.email().trim().toLowerCase();
        String domainSuffix = "@" + EmailDomainUtils.extractDomain(email);

        if (usuarioRepository.existsByEmail(email)) {
            throw new ConflictException("Email já está em uso");
        }

        var contratadaExistenteMesmoDominio = usuarioRepository
                .findByRoleAndEmailEndingWithIgnoreCase(Role.CONTRATADA, domainSuffix)
                .stream()
                .map(Usuario::getContratada)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        if (contratadaExistenteMesmoDominio != null) {
            if (contratadaExistenteMesmoDominio.getContratante() == null
                    || !contratadaExistenteMesmoDominio.getContratante().getId().equals(contratante.getId())) {
                throw new ForbiddenException("Dominio já vinculado a uma contratada de outra contratante");
            }

            Usuario usuarioAdicional = Usuario.builder()
                    .email(email)
                    .senha(passwordEncoder.encode(request.senha()))
                    .role(Role.CONTRATADA)
                    .mustChangePassword(true)
                    .contratada(contratadaExistenteMesmoDominio)
                    .build();
            usuarioRepository.save(usuarioAdicional);
            return ContratadaResponse.fromEntity(contratadaExistenteMesmoDominio);
        }

        String cnpjNormalizado = normalizarCnpj(request.cnpj());
        if (cnpjNormalizado == null || cnpjNormalizado.isBlank()) {
            throw new ConflictException("CNPJ é obrigatório para criar nova organização contratada");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.CONTRATADA)
                .mustChangePassword(true)
                .build();

        Usuario usuarioSalvo = usuarioRepository.save(usuario);

        Contratada contratada = Contratada.builder()
                .nome(request.nome())
                .cnpj(cnpjNormalizado)
                .numeroContrato(request.numeroContrato())
                .numeroPedido(request.numeroPedido())
                .contratante(contratante)
                .usuario(usuarioSalvo)
                .build();

        Contratada saved = contratadaRepository.save(contratada);
        usuarioSalvo.setContratada(saved);
        usuarioRepository.save(usuarioSalvo);

        return ContratadaResponse.fromEntity(saved);
    }

    public Page<ContratadaResponse> listarTodas(int page) {
        return contratadaRepository.findAll(PageRequest.of(page, 10))
                .map(ContratadaResponse::fromEntity);
    }

    @Auditavel(acao = "CONTRATADA_ATUALIZADA", entidade = "Contratada")
    @Transactional
    public ContratadaResponse atualizar(Long id, UpdateContratadaRequest request) {

        Contratada contratada = contratadaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contratada não encontrada"));

        Usuario usuarioLogado = usuarioLogadoService.getUsuario();
        if (usuarioLogado.getRole() != Role.ADMIN) {
            Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
            if (contratada.getContratante() == null
                    || !contratada.getContratante().getId().equals(contratanteLogado.getId())) {
                throw new ForbiddenException("Você não tem permissão para editar esta contratada");
            }
        }

        Usuario usuario = contratada.getUsuario();
        if (usuario == null && contratada.getUsuarios() != null && !contratada.getUsuarios().isEmpty()) {
            usuario = contratada.getUsuarios().get(0);
        }
        if (usuario == null) {
            throw new ResourceNotFoundException("Usuário da contratada não encontrado");
        }

        String novoEmail = request.email().trim();
        if (!novoEmail.equalsIgnoreCase(usuario.getEmail())
                && usuarioRepository.existsByEmail(novoEmail)) {
            throw new ConflictException("Email já está em uso");
        }

        contratada.setNome(request.nome().trim());
        contratada.setCnpj(normalizarCnpj(request.cnpj()));
        contratada.setNumeroContrato(request.numeroContrato().trim());
        contratada.setNumeroPedido(request.numeroPedido().trim());

        usuario.setEmail(novoEmail);
        usuario.setNome(request.nome().trim());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.senha()));
        }

        usuarioRepository.save(usuario);
        Contratada saved = contratadaRepository.save(contratada);
        return ContratadaResponse.fromEntity(saved);
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null) return null;
        return cnpj.replaceAll("\\D", "");
    }

    @Auditavel(acao = "CONTRATADA_EXCLUIDA", entidade = "Contratada")
    @Transactional
    public void deletarContratada(Long id) {

        Contratada contratada = contratadaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contratada não encontrada"));

        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN) {
            contratadaRepository.delete(contratada);
            return;
        }

        Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();

        if (!contratada.getContratante().getId().equals(contratanteLogado.getId())) {
            throw new ForbiddenException("Você não tem permissão para deletar esta contratada");
        }

        contratadaRepository.delete(contratada);
    }

    public void removerPorNome(String nome) {
        Contratada contratada = contratadaRepository
                .findByNome(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Contratada não encontrada"));

        contratadaRepository.delete(contratada);
    }
}
