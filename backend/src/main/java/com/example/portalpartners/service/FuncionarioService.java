package com.example.portalpartners.service;

import com.example.portalpartners.audit.Auditavel;
import com.example.portalpartners.crypto.FieldEncryptionService;
import com.example.portalpartners.dto.CreateFuncionarioRequest;
import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.exceptions.ConflictException;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.exceptions.ResourceNotFoundException;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Funcionario;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FuncionarioService {

    private final FuncionarioRepository funcionarioRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final FieldEncryptionService fieldEncryptionService;

    @Auditavel(acao = "FUNCIONARIO_CRIADO", entidade = "Funcionario")
    @Transactional
    public FuncionarioResponse criar(CreateFuncionarioRequest request) {

        Contratada contratada = usuarioLogadoService.getContratadaLogada();
        String cpfNormalizado = normalizarCpf(request.cpf());

        // Unicidade via cpfHash (HMAC deterministico — nao expoe o CPF real)
        String cpfHash = fieldEncryptionService.hash(cpfNormalizado);
        if (funcionarioRepository.existsByCpfHashAndContratada(cpfHash, contratada)) {
            throw new ConflictException("Funcionário com este CPF já existe nesta contratada");
        }

        Funcionario funcionario = Funcionario.builder()
                .nomeCompleto(request.nomeCompleto())
                .cpf(cpfNormalizado)        // cifrado pelo EncryptedStringConverter
                .cpfHash(cpfHash)           // HMAC para busca/unicidade
                .contratada(contratada)
                .build();

        funcionarioRepository.save(funcionario);
        return FuncionarioResponse.fromEntity(funcionario);
    }

    public Page<FuncionarioResponse> listarPaginado(int page, int size) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN) {
            return funcionarioRepository.findAll(PageRequest.of(page, size))
                    .map(FuncionarioResponse::fromEntity);
        }

        Contratada contratada = usuarioLogadoService.getContratadaLogada();
        return funcionarioRepository.findByContratada(contratada, PageRequest.of(page, size))
                .map(FuncionarioResponse::fromEntity);
    }

    public List<FuncionarioResponse> listar() {
        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN) {
            return funcionarioRepository.findAll()
                    .stream()
                    .map(FuncionarioResponse::fromEntity)
                    .toList();
        }

        Contratada contratada = usuarioLogadoService.getContratadaLogada();
        return funcionarioRepository.findByContratada(contratada);
    }

    public List<FuncionarioResponse> findByContratada(String nome) {
        Contratada contratada = usuarioLogadoService.getContratadaLogada();
        return funcionarioRepository.findByContratada(contratada);
    }

    public FuncionarioResponse buscarFuncionarioPorNomeCompleto(String nomeCompleto) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        Funcionario funcionario;
        if (usuario.getRole() == Role.ADMIN) {
            funcionario = funcionarioRepository
                    .findFirstByNomeCompletoContainingIgnoreCase(nomeCompleto);
        } else {
            Contratada contratada = usuarioLogadoService.getContratadaLogada();
            funcionario = funcionarioRepository
                    .findByContratadaAndNomeCompletoContainingIgnoreCase(contratada, nomeCompleto);
        }

        if (funcionario == null) {
            throw new ResourceNotFoundException("Funcionário não encontrado");
        }
        return FuncionarioResponse.fromEntity(funcionario);
    }

    @Auditavel(acao = "FUNCIONARIO_EXCLUIDO", entidade = "Funcionario")
    @Transactional
    public void deletarFuncionario(Long id) {

        Funcionario funcionario = funcionarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));

        Usuario usuario = usuarioLogadoService.getUsuario();

        if (usuario.getRole() == Role.ADMIN) {
            funcionarioRepository.delete(funcionario);
            return;
        }

        if (usuario.getRole() == Role.CONTRATADA) {
            Contratada contratadaLogada = usuarioLogadoService.getContratadaLogada();
            if (!funcionario.getContratada().getId().equals(contratadaLogada.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
            funcionarioRepository.delete(funcionario);
            return;
        }

        if (usuario.getRole() == Role.CONTRATANTE) {
            Contratante contratanteLogado = usuarioLogadoService.getContratanteLogada();
            if (!funcionario.getContratada().getContratante().getId()
                    .equals(contratanteLogado.getId())) {
                throw new ForbiddenException("Você não tem permissão");
            }
            funcionarioRepository.delete(funcionario);
            return;
        }

        throw new ForbiddenException("Perfil sem permissão");
    }

    private String normalizarCpf(String cpf) {
        if (cpf == null) return null;
        return cpf.replaceAll("\\D", "");
    }
}
