package com.example.portalpartners.controller;

import com.example.portalpartners.documento.DocumentoSpecification;
import com.example.portalpartners.dto.DocumentoResponse;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.Funcionario;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import com.example.portalpartners.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final UsuarioLogadoService usuarioLogadoService;
    private final DocumentoRepository documentoRepository;
    private final ContratadaRepository contratadaRepository;
    private final FuncionarioRepository funcionarioRepository;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CONTRATANTE', 'ROLE_CONTRATADA', 'ROLE_ADMIN')")
    public SearchResponse search(@RequestParam("q") String q) {
        String query = q == null ? "" : q.trim();
        if (query.isBlank()) {
            return new SearchResponse(List.of(), List.of(), List.of());
        }

        Usuario usuario = usuarioLogadoService.getUsuario();
        Role role = usuario.getRole();

        Pageable top10 = PageRequest.of(0, 10);

        Specification<Documento> spec = Specification.where(null);
        if (role == Role.CONTRATADA) {
            spec = spec.and(DocumentoSpecification.porContratadaId(usuarioLogadoService.getContratadaLogada().getId()));
        } else if (role == Role.CONTRATANTE) {
            spec = spec.and(DocumentoSpecification.porContratanteId(usuarioLogadoService.getContratanteLogada().getId()));
        }

        Specification<Documento> byNomeArquivo = (root, cq, cb) -> cb.like(
                cb.lower(root.get("nomeArquivo")),
                "%" + query.toLowerCase() + "%"
        );

        List<DocumentoResponse> documentos = documentoRepository.findAll(spec.and(byNomeArquivo), top10)
                .map(DocumentoResponse::fromEntity)
                .getContent();

        List<ContratadaResult> contratadas;
        if (role == Role.CONTRATANTE) {
            Contratante contratante = usuarioLogadoService.getContratanteLogada();
            contratadas = contratadaRepository
                    .findTop10ByContratanteAndNomeContainingIgnoreCaseOrderByNomeAsc(contratante, query)
                    .stream()
                    .map(c -> new ContratadaResult(c.getId(), c.getNome(), c.getCnpj()))
                    .toList();
        } else if (role == Role.ADMIN) {
            contratadas = contratadaRepository
                    .findTop10ByNomeContainingIgnoreCaseOrderByNomeAsc(query)
                    .stream()
                    .map(c -> new ContratadaResult(c.getId(), c.getNome(), c.getCnpj()))
                    .toList();
        } else {
            Contratada c = usuarioLogadoService.getContratadaLogada();
            contratadas = List.of(new ContratadaResult(c.getId(), c.getNome(), c.getCnpj()));
        }

        List<FuncionarioResult> funcionarios;
        if (role == Role.CONTRATADA) {
            Contratada contratada = usuarioLogadoService.getContratadaLogada();
            funcionarios = funcionarioRepository
                    .findTop10ByContratadaAndNomeCompletoContainingIgnoreCaseOrderByNomeCompletoAsc(contratada, query)
                    .stream()
                    .map(f -> new FuncionarioResult(f.getId(), f.getNomeCompleto(), f.getCpf()))
                    .toList();
        } else if (role == Role.ADMIN) {
            funcionarios = funcionarioRepository
                    .findTop10ByNomeCompletoContainingIgnoreCaseOrderByNomeCompletoAsc(query)
                    .stream()
                    .map(f -> new FuncionarioResult(f.getId(), f.getNomeCompleto(), f.getCpf()))
                    .toList();
        } else {
            // CONTRATANTE: não temos relação direta contratante->funcionarios sem passar por contratadas,
            // então retornamos apenas busca por documentos (funcionários aparecem no resultado do documento).
            funcionarios = List.of();
        }

        return new SearchResponse(documentos, contratadas, funcionarios);
    }

    public record ContratadaResult(Long id, String nome, String cnpj) {}

    public record FuncionarioResult(Long id, String nomeCompleto, String cpf) {}

    public record SearchResponse(
            List<DocumentoResponse> documentos,
            List<ContratadaResult> contratadas,
            List<FuncionarioResult> funcionarios
    ) {}
}
