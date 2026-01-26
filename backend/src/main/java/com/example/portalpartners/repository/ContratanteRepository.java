package com.example.portalpartners.repository;

import com.example.portalpartners.model.Contratante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratanteRepository extends JpaRepository<Contratante, Long> {
    boolean existsByNome(String nome);
    Optional<Contratante> findByNome(String contratanteNome);
}