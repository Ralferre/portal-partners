package com.example.portalpartners.config;

import com.example.portalpartners.AbstractIntegrationTest;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica o seeder de documentos de demonstracao:
 *  - cria exatamente 10 documentos (5 da contratada, 5 do funcionario);
 *  - inclui os tipos CND estendidos;
 *  - e idempotente (segunda execucao nao duplica);
 *  - NAO cria nem altera usuarios (invariante de seguranca critica).
 */
@TestPropertySource(properties = "app.demo.docs.enabled=true")
class DemoDocumentosSeederIntegrationTest extends AbstractIntegrationTest {

    @Autowired DemoDocumentosSeeder seeder;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ContratanteRepository contratanteRepository;
    @Autowired ContratadaRepository contratadaRepository;
    @Autowired FuncionarioRepository funcionarioRepository;
    @Autowired DocumentoRepository documentoRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private long usuariosAntes;

    @BeforeEach
    void setUp() {
        documentoRepository.deleteAll();
        funcionarioRepository.deleteAll();
        contratadaRepository.deleteAll();
        contratanteRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario uContratante = usuarioRepository.save(Usuario.builder()
                .nome("Contratante").email("ct@contratante.com")
                .senha(passwordEncoder.encode("x")).role(Role.CONTRATANTE).build());
        Contratante contratante = contratanteRepository.save(Contratante.builder()
                .nome("Contratante").cnpj("11111111000111").dominioEmail("contratante.com")
                .usuario(uContratante).build());

        Usuario uContratada = usuarioRepository.save(Usuario.builder()
                .nome("Contratada Demo").email("demo@contratada.com")
                .senha(passwordEncoder.encode("x")).role(Role.CONTRATADA).build());
        contratadaRepository.save(Contratada.builder()
                .nome("Contratada Demo").cnpj("22222222000122").numeroContrato("1").numeroPedido("1")
                .usuario(uContratada).contratante(contratante).build());

        usuariosAntes = usuarioRepository.count();
    }

    @Test
    void seed_criaDezDocumentos_comSplitCorreto_eNaoAlteraUsuarios() {
        seeder.seed();

        assertThat(documentoRepository.count()).isEqualTo(10);

        var todos = documentoRepository.findAll();
        long daContratada = todos.stream().filter(d -> d.getFuncionario() == null).count();
        long doFuncionario = todos.stream().filter(d -> d.getFuncionario() != null).count();
        assertThat(daContratada).isEqualTo(5);
        assertThat(doFuncionario).isEqualTo(5);

        // Tipos CND estendidos presentes.
        assertThat(todos.stream().map(Documento::getTipoDocumento))
                .contains(TipoDocumento.CND_FEDERAL, TipoDocumento.CND_ESTADUAL, TipoDocumento.CND_MUNICIPAL);

        // Status variados para o semaforo.
        assertThat(todos.stream().map(Documento::getStatusDocumento).distinct())
                .contains(StatusDocumento.APROVADO, StatusDocumento.PENDENTE, StatusDocumento.REPROVADO);

        // INVARIANTE: nenhum usuario foi criado ou removido.
        assertThat(usuarioRepository.count()).isEqualTo(usuariosAntes);
    }

    @Test
    void seed_eIdempotente() {
        seeder.seed();
        long apos1 = documentoRepository.count();
        seeder.seed();
        long apos2 = documentoRepository.count();
        assertThat(apos2).isEqualTo(apos1);
    }
}
