package com.example.portalpartners.service;

import com.example.portalpartners.AbstractIntegrationTest;
import com.example.portalpartners.dto.SolicitarUploadRequest;
import com.example.portalpartners.dto.TypeReferenceFile;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.exceptions.ForbiddenException;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.ContratanteRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testes de regra de negocio critica do DocumentoService:
 *  - isolamento multi-tenant (uma CONTRATADA nao baixa documento de outra);
 *  - exigencia de consentimento LGPD para documento de funcionario.
 *
 * UsuarioLogadoService e MinioService sao mockados para controlar o usuario
 * autenticado e evitar rede; o restante roda contra Postgres real.
 */
class DocumentoServicePermissaoIntegrationTest extends AbstractIntegrationTest {

    @Autowired DocumentoService documentoService;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ContratanteRepository contratanteRepository;
    @Autowired ContratadaRepository contratadaRepository;
    @Autowired DocumentoRepository documentoRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @MockBean UsuarioLogadoService usuarioLogadoService;
    @MockBean MinioService minioService;

    private Contratada contratadaA;
    private Contratada contratadaB;
    private Usuario usuarioContratadaB;
    private Documento docDaContratadaA;

    @BeforeEach
    void setUp() {
        documentoRepository.deleteAll();
        contratadaRepository.deleteAll();
        contratanteRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario uContratante = usuarioRepository.save(Usuario.builder()
                .nome("Contratante").email("ct@contratante.com")
                .senha(passwordEncoder.encode("x")).role(Role.CONTRATANTE).build());
        Contratante contratante = contratanteRepository.save(Contratante.builder()
                .nome("Contratante").cnpj("11111111000111").dominioEmail("contratante.com")
                .usuario(uContratante).build());

        Usuario uA = usuarioRepository.save(Usuario.builder()
                .nome("Contratada A").email("a@a.com")
                .senha(passwordEncoder.encode("x")).role(Role.CONTRATADA).build());
        contratadaA = contratadaRepository.save(Contratada.builder()
                .nome("Contratada A").cnpj("22222222000122").numeroContrato("1").numeroPedido("1")
                .usuario(uA).contratante(contratante).build());

        usuarioContratadaB = usuarioRepository.save(Usuario.builder()
                .nome("Contratada B").email("b@b.com")
                .senha(passwordEncoder.encode("x")).role(Role.CONTRATADA).build());
        contratadaB = contratadaRepository.save(Contratada.builder()
                .nome("Contratada B").cnpj("33333333000133").numeroContrato("2").numeroPedido("2")
                .usuario(usuarioContratadaB).contratante(contratante).build());

        docDaContratadaA = documentoRepository.save(buildDoc(contratadaA));
    }

    private Documento buildDoc(Contratada dona) {
        Documento d = new Documento();
        d.setTipoDocumento(TipoDocumento.CNPJ);
        d.setNomeArquivo("documento.pdf");
        d.setStatusDocumento(StatusDocumento.PENDENTE);
        d.setDataPostagem(LocalDateTime.now());
        d.setObjectKey("key-" + dona.getCnpj());
        d.setContratada(dona);
        return d;
    }

    @Test
    void contratadaB_naoPodeBaixarDocumentoDaContratadaA() {
        // Usuario logado = Contratada B tentando baixar doc da Contratada A.
        when(usuarioLogadoService.getUsuario()).thenReturn(usuarioContratadaB);
        when(usuarioLogadoService.getContratadaLogada()).thenReturn(contratadaB);

        assertThatThrownBy(() -> documentoService.solicitarDownload(docDaContratadaA.getId()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void uploadDeFuncionario_semConsentimentoLgpd_eBloqueado() {
        // Contratada A autenticada; sem registro de consentimento LGPD.
        Usuario uA = usuarioRepository.findByEmail("a@a.com").orElseThrow();
        when(usuarioLogadoService.getUsuario()).thenReturn(uA);
        when(usuarioLogadoService.getContratadaLogada()).thenReturn(contratadaA);

        SolicitarUploadRequest req = new SolicitarUploadRequest(
                "aso.pdf", "application/pdf", 1234L,
                TipoDocumento.ASO, TypeReferenceFile.FUNCIONARIO, 999L, null);

        // Documento de FUNCIONARIO exige consentimento LGPD valido -> bloqueia.
        assertThatThrownBy(() -> documentoService.solicitarUpload(req))
                .isInstanceOf(BusinessRulesException.class);
    }

    @Test
    void listarTiposDocumento_incluiNovosTiposCnd() {
        assertThat(documentoService.listarTiposDocumento())
                .contains(TipoDocumento.CND_FEDERAL, TipoDocumento.CND_ESTADUAL, TipoDocumento.CND_MUNICIPAL);
    }
}
