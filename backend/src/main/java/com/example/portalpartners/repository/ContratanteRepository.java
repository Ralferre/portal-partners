package com.example.portalpartners.repository;

import com.example.portalpartners.model.Contratante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContratanteRepository extends JpaRepository<Contratante, Long> {
    boolean existsByNome(String nome);
}