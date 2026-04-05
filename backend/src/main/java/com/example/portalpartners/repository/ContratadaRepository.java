package com.example.portalpartners.repository;

import com.example.portalpartners.dto.ContratadaResponse;
import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratadaRepository extends JpaRepository<Contratada, Long> {
    boolean existsByUsuario(Usuario usuario);

    Optional<Contratada> findByNome(String nome);

    Optional<Contratada> findByNomeIgnoreCase(String contratada);

    Optional<Contratada> findByUsuarioId(Long usuarioId);

    Page<Contratada> findByContratante(
            Contratante contratante,
            Pageable pageable
    );

    Optional<Contratada> findByUsuario(Usuario usuario);

    Optional<Contratada> findByContratanteAndNomeContainingIgnoreCase(
            Contratante contratante,
            String nome
    );

    List<Contratada> findTop10ByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    List<Contratada> findTop10ByContratanteAndNomeContainingIgnoreCaseOrderByNomeAsc(
            Contratante contratante,
            String nome
    );

    @Query("""
            select c
            from Contratada c
            where lower(c.usuario.email) like lower(concat('%', :domainSuffix))
            order by c.id asc
            """)
    List<Contratada> findAllByUsuarioEmailDomain(@Param("domainSuffix") String domainSuffix);

}