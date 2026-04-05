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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DataSeeder {
    private final UsuarioRepository usuarioRepository;
    private final ContratanteRepository contratanteRepository;
    private final ContratadaRepository contratadaRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.contratante-password:}")
    private String contratantePassword;

    @Value("${app.seed.contratada-password:}")
    private String contratadaPassword;

    @PostConstruct
    @Transactional
    public void seed() {
        if (usuarioRepository.count() > 0 || contratanteRepository.count() > 0 || contratadaRepository.count() > 0) {
            return;
        }

        String resolvedAdminPassword = requireSeedPassword(adminPassword, "app.seed.admin-password");
        String resolvedContratantePassword = requireSeedPassword(contratantePassword, "app.seed.contratante-password");
        String resolvedContratadaPassword = requireSeedPassword(contratadaPassword, "app.seed.contratada-password");

        Usuario admin = usuarioRepository.save(
                Usuario.builder()
                        .nome("Administrador")
                        .email("admin@admin.com")
                        .senha(passwordEncoder.encode(resolvedAdminPassword))
                        .role(Role.ADMIN)
                        .mustChangePassword(false)
                        .build()
        );

        Usuario usuarioContratante = usuarioRepository.save(
                Usuario.builder()
                        .nome("Empresa Contratante")
                        .email("contratante@contratante.com")
                        .senha(passwordEncoder.encode(resolvedContratantePassword))
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
                        .senha(passwordEncoder.encode(resolvedContratadaPassword))
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

    private String requireSeedPassword(String password, String propertyName) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "Propriedade obrigatoria ausente para seed: " + propertyName +
                    ". Defina essa propriedade apenas no ambiente em que app.seed.enabled=true."
            );
        }
        return password;
    }
}

