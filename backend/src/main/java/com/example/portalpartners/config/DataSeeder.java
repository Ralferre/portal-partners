package com.example.portalpartners.config;

import com.example.portalpartners.model.Contratada;
import com.example.portalpartners.model.Contratante;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class DataSeeder {
    private final UsuarioRepository usuarioRepository;
    private final ContratanteRepository contratanteRepository;
    private final ContratadaRepository contratadaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    @Transactional
    public void seed() {
        if (usuarioRepository.count() > 0 || contratanteRepository.count() > 0 || contratadaRepository.count() > 0) {
            return;
        }

        Usuario admin = usuarioRepository.save(
                Usuario.builder()
                        .nome("Administrador")
                        .email("admin@admin.com")
                        .senha(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .mustChangePassword(false)
                        .build()
        );

        Usuario usuarioContratante = usuarioRepository.save(
                Usuario.builder()
                        .nome("Empresa Contratante")
                        .email("contratante@contratante.com")
                        .senha(passwordEncoder.encode("contratante123"))
                        .role(Role.CONTRATANTE)
                        .mustChangePassword(false)
                        .build()
        );

        Contratante contratante = contratanteRepository.save(
                Contratante.builder()
                        .nome("Empresa Contratante")
                        .cnpj("82197557000129")
                        .dominioEmail("contratante.com")
                        .usuario(usuarioContratante)
                        .build()
        );

        usuarioContratante.setContratante(contratante);
        usuarioRepository.save(usuarioContratante);

        Usuario usuarioContratada = usuarioRepository.save(
                Usuario.builder()
                        .nome("Empresa Contratada")
                        .email("contratada@contratada.com")
                        .senha(passwordEncoder.encode("contratada123"))
                        .role(Role.CONTRATADA)
                        .mustChangePassword(false)
                        .build()
        );

        Contratada contratada = contratadaRepository.save(
                Contratada.builder()
                        .nome("Empresa Contratada")
                        .cnpj("02596263000130")
                        .numeroContrato("8897")
                        .numeroPedido("8897")
                        .usuario(usuarioContratada)
                        .contratante(contratante)
                        .build()
        );

        usuarioContratada.setContratada(contratada);
        usuarioRepository.save(usuarioContratada);
    }
}

