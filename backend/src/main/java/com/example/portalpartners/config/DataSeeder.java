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


@Component
@RequiredArgsConstructor
public class DataSeeder {
    private final UsuarioRepository usuarioRepository;
    private final ContratanteRepository contratanteRepository;
    private final ContratadaRepository contratadaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostConstruct
    public void seed() {
        // ADMIN
        Usuario admin = usuarioRepository.findByEmail("admin@admin.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .email("admin@admin.com")
                                .senha(passwordEncoder.encode("admin123"))
                                .role(Role.ADMIN)
                                .build()
                ));

        // CONTRATANTE
        Usuario uContratante = usuarioRepository.findByEmail("contratante@empresa.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .email("contratante@empresa.com")
                                .senha(passwordEncoder.encode("contratante123"))
                                .role(Role.CONTRATANTE)
                                .build()
                ));

        Contratante contratante = contratanteRepository
                .findByUsuario(uContratante)
                .orElseGet(() -> contratanteRepository.save(
                        Contratante.builder()
                                .nome("Empresa Contratante Teste")
                                .usuario(uContratante)
                                .build()
                ));

        // CONTRATADA
        Usuario uContratada = usuarioRepository.findByEmail("contratada@empresa.com")
                .orElseGet(() -> usuarioRepository.save(
                        Usuario.builder()
                                .email("contratada@empresa.com")
                                .senha(passwordEncoder.encode("empresa123"))
                                .role(Role.CONTRATADA)
                                .build()
                ));

        if (!contratadaRepository.existsByUsuario(uContratada)) {
            contratadaRepository.save(
                    Contratada.builder()
                            .nome("Empresa Contratada Teste")
                            .cnpj("12.345.678/0001-99")
                            .numeroContrato("8897")
                            .numeroPedido("8897")
                            .usuario(uContratada)
                            .contratante(contratante)
                            .build()
            );
        }
    }
}

