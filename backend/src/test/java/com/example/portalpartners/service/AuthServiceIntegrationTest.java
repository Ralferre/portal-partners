package com.example.portalpartners.service;

import com.example.portalpartners.AbstractIntegrationTest;
import com.example.portalpartners.dto.AuthRequest;
import com.example.portalpartners.dto.AuthResponse;
import com.example.portalpartners.dto.ForgotPasswordRequest;
import com.example.portalpartners.dto.ResetPasswordRequest;
import com.example.portalpartners.model.Role;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

/**
 * Testes de integracao do fluxo de autenticacao: login, recuperacao de senha
 * (forgot/reset com token) e troca de senha. Usa Postgres real (Testcontainers);
 * o EmailService e mockado para nao abrir SMTP.
 */
class AuthServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired AuthService authService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    // Repositorios dependentes: limpos antes de usuario para respeitar as FKs,
    // ja que outros testes podem deixar contratante/contratada/documento.
    @Autowired DocumentoRepository documentoRepository;
    @Autowired FuncionarioRepository funcionarioRepository;
    @Autowired ContratadaRepository contratadaRepository;
    @Autowired ContratanteRepository contratanteRepository;

    @MockBean EmailService emailService;

    private Usuario admin;

    @BeforeEach
    void setUp() {
        documentoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratadaRepository.deleteAll();
        contratanteRepository.deleteAll();
        usuarioRepository.deleteAll();
        admin = usuarioRepository.save(Usuario.builder()
                .nome("Admin Teste")
                .email("admin-teste@example.com")
                .senha(passwordEncoder.encode("SenhaForte123"))
                .role(Role.ADMIN)
                .mustChangePassword(false)
                .build());
    }

    @Test
    void login_comCredenciaisValidas_retornaToken() {
        AuthRequest req = new AuthRequest();
        req.setEmail("admin-teste@example.com");
        req.setSenha("SenhaForte123");

        AuthResponse resp = authService.login(req);

        assertThat(resp.token()).isNotBlank();
        assertThat(resp.email()).isEqualTo("admin-teste@example.com");
        assertThat(resp.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void login_comSenhaInvalida_lancaExcecao() {
        AuthRequest req = new AuthRequest();
        req.setEmail("admin-teste@example.com");
        req.setSenha("senha-errada");

        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void forgotPassword_usuarioExistente_gravaTokenEEnviaEmail() throws Exception {
        authService.forgotPassword(new ForgotPasswordRequest("admin-teste@example.com"));

        Usuario atualizado = usuarioRepository.findByEmail("admin-teste@example.com").orElseThrow();
        assertThat(atualizado.getResetToken()).isNotBlank();
        assertThat(atualizado.getResetTokenExpiration()).isNotNull();
        verify(emailService).sendResetPasswordEmail(org.mockito.ArgumentMatchers.eq("admin-teste@example.com"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void forgotPassword_usuarioInexistente_naoVazaExistencia() throws Exception {
        String msg = authService.forgotPassword(new ForgotPasswordRequest("naoexiste@example.com"));
        // Resposta generica identica, sem revelar que o email nao existe.
        assertThat(msg).contains("Se o email existir");
    }

    @Test
    void resetPassword_comTokenValido_alteraSenha() throws Exception {
        authService.forgotPassword(new ForgotPasswordRequest("admin-teste@example.com"));
        String token = usuarioRepository.findByEmail("admin-teste@example.com").orElseThrow().getResetToken();

        authService.resetPassword(new ResetPasswordRequest(
                "admin-teste@example.com", token, "NovaSenha456"));

        Usuario atualizado = usuarioRepository.findByEmail("admin-teste@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("NovaSenha456", atualizado.getSenha())).isTrue();
        assertThat(atualizado.getResetToken()).isNull();
    }

    @Test
    void resetPassword_comTokenInvalido_lancaExcecao() {
        assertThatThrownBy(() -> authService.resetPassword(new ResetPasswordRequest(
                "admin-teste@example.com", "token-invalido", "NovaSenha456")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
