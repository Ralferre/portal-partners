package com.example.portalpartners.repository;

import com.example.portalpartners.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    boolean existsByCpf(String cpf);
    Optional<Funcionario> findByNomeCompleto(String nomeCompleto);

    Optional<Funcionario> findByNomeCompletoIgnoreCase(String nomeCompleto);
}