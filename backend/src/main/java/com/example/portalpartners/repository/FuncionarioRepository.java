package com.example.portalpartners.repository;

import com.example.portalpartners.dto.FuncionarioResponse;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Funcionario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {

    /** @deprecated Usar existsByCpfHashAndContratada para dados criptografados. */
    @Deprecated
    boolean existsByCpf(String cpf);

    /** @deprecated Usar existsByCpfHashAndContratada para dados criptografados. */
    @Deprecated
    boolean existsByCpfAndContratada(String cpf, Contratada contratada);

    /**
     * Verifica existencia via HMAC-SHA256 do CPF.
     * Deterministico: o mesmo CPF normalizado sempre gera o mesmo hash.
     */
    boolean existsByCpfHashAndContratada(String cpfHash, Contratada contratada);

    Optional<Funcionario> findByNomeCompleto(String nomeCompleto);

    Optional<Funcionario> findByNomeCompletoIgnoreCase(String nomeCompleto);

    List<FuncionarioResponse> findByContratada(Contratada contratada);

    Page<Funcionario> findByContratada(Contratada contratada, Pageable pageable);

    Funcionario findByContratadaAndNomeCompletoContainingIgnoreCase(
            Contratada contratada,
            String nomeCompleto
    );

    Funcionario findFirstByNomeCompletoContainingIgnoreCase(String nomeCompleto);

    List<Funcionario> findTop10ByNomeCompletoContainingIgnoreCaseOrderByNomeCompletoAsc(String nomeCompleto);

    List<Funcionario> findTop10ByContratadaAndNomeCompletoContainingIgnoreCaseOrderByNomeCompletoAsc(
            Contratada contratada,
            String nomeCompleto
    );
}
