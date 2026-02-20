package com.example.portalpartners.repository;

import com.example.portalpartners.model.Documento;
import com.example.portalpartners.model.StatusDocumento;
import com.example.portalpartners.model.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DocumentoRepository extends JpaRepository<Documento, Long>, JpaSpecificationExecutor<Documento> {
    List<Documento> findTop10ByOrderByDataPostagemDesc();
    long countByStatusDocumento(StatusDocumento statusDocumento);
    long countByFuncionarioId(Long funcionarioId);
    long countByStatusDocumentoAndContratadaId(StatusDocumento statusDocumento, Long contratadaId);
    long countByStatusDocumentoAndFuncionarioId(StatusDocumento statusDocumento, Long funcionarioId);
    List<Documento> findByContratadaNome(String contratadaNome);
    List<Documento> findByFuncionarioId(Long funcionarioId);
    List<Documento> findByContratadaNomeAndTipoDocumento(String contratadaNome, TipoDocumento tipoDocumento);

    Page<Documento> findByFuncionarioNomeCompletoContainingIgnoreCase(
            String nomeCompleto,
            Pageable pageable
    );

    Page<Documento> findByNomeArquivoContainingIgnoreCase(
            String nomeArquivo,
            Pageable pageable
    );
}
