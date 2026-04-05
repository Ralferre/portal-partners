package com.example.portalpartners.repository;

import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmail(String email);
    Optional<Usuario> findByEmail(String email);
    List<Usuario> findByRoleAndEmailEndingWithIgnoreCase(Role role, String domainSuffix);
    List<Usuario> findByContratanteIdOrderByIdAsc(Long contratanteId);
}