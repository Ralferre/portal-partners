package com.example.portalpartners.repository;

import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.StatusDocumento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findTop10ByOrderByDataPostagemDesc();
    long countByStatus(StatusDocumento status);


    long countByFuncionarioId(Long funcionarioId);
    long countByStatusAndContratadaId(StatusDocumento status, Long contratadaId);
    long countByStatusAndFuncionarioId(StatusDocumento status, Long funcionarioId);
    List<Documento> findByContratadaId(Long contratadaId);
    List<Documento> findByFuncionarioId(Long funcionarioId);

    List<Documento> findByContratadaIdAndTipo(Long contratadaId, String tipo);
}
