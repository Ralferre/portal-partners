package com.example.portalpartners.repository;

import com.example.portalpartners.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findTop10ByOrderByDataPostagemDesc();
    long countByStatus(String status);
    long countByContratadaId(Long contratadaId);
    long countByFuncionarioId(Long funcionarioId);
    long countByStatusAndContratadaId(String status, Long contratadaId);
    long countByStatusAndFuncionarioId(String status, Long funcionarioId);
    List<Documento> findByContratadaId(Long contratadaId);
    List<Documento> findByFuncionarioId(Long funcionarioId);
}
