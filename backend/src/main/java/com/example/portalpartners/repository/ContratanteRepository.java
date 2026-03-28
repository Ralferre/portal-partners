package com.example.portalpartners.repository;

import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratanteRepository extends JpaRepository<Contratante, Long> {
    boolean existsByNome(String nome);
    boolean existsByCnpj(String cnpj);
    boolean existsByDominioEmail(String dominioEmail);
    Optional<Contratante> findByNome(String contratanteNome);
    Optional<Contratante> findByCnpj(String cnpj);
    Optional<Contratante> findByDominioEmail(String dominioEmail);

    boolean existsByUsuario(Usuario usuario);

    Optional<Contratante> findByUsuarioId(Long usuarioId);

    Optional<Contratante> findByUsuario(Usuario usuario);

    @Query("""
            select c
            from Contratante c
            where lower(c.usuario.email) like lower(concat('%', :domainSuffix))
            order by c.id asc
            """)
    List<Contratante> findAllByUsuarioEmailDomain(@Param("domainSuffix") String domainSuffix);

}