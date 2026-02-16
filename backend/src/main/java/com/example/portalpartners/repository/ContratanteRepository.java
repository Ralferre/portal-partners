package com.example.portalpartners.repository;

import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContratanteRepository extends JpaRepository<Contratante, Long> {
    boolean existsByNome(String nome);
    Optional<Contratante> findByNome(String contratanteNome);

    boolean existsByUsuario(Usuario usuario);

//    Contratante findByEmail(String email);

    Optional<Contratante> findByUsuarioId(Long usuarioId);

    Optional<Contratante> findByUsuario(Usuario usuario);

}