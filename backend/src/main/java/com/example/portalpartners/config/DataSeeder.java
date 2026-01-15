package com.example.portalpartners.config;

import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder {
    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void seed() {
        if (!usuarioRepository.existsByEmail("admin@admin.com")) {
            usuarioRepository.save(
                Usuario.builder()
                    .email("admin@admin.com")
                    .senha(passwordEncoder.encode("admin123"))
                    .tipo("CONTRATANTE")
                    .build()
            );
        }
        if (!usuarioRepository.existsByEmail("contratada@empresa.com")) {
            usuarioRepository.save(
                Usuario.builder()
                    .email("contratada@empresa.com")
                    .senha(passwordEncoder.encode("empresa123"))
                    .tipo("CONTRATADA")
                    .build()
            );
        }
    }
}
