package com.example.portalpartners.repository;

import com.example.portalpartners.model.Contratada;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratadaRepository extends JpaRepository<Contratada, Long> {
    boolean existsByCnpj(String cnpj);
    Optional<Contratada> findByNome(String nome);
    Optional<Contratada> findByNomeIgnoreCase(String contratadaNome);
}