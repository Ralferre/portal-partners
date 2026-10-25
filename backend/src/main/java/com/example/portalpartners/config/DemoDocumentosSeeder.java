package com.example.portalpartners.config;

import com.example.portalpartners.crypto.FieldEncryptionService;
import com.example.portalpartners.model.*;
import com.example.portalpartners.repository.ContratadaRepository;
import com.example.portalpartners.repository.DocumentoRepository;
import com.example.portalpartners.repository.FuncionarioRepository;
import com.example.portalpartners.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Semeia os 10 documentos de demonstracao (PDFs ficticios 4dev) para popular
 * dashboards e tabelas em apresentacoes comerciais.
 *
 * SEGURANCA / INVARIANTES:
 *  - ADITIVO: nunca cria nem altera USUARIOS. Opera sobre a Contratada que ja
 *    existe (criada pelo usuario) e, se necessario, cria apenas um Funcionario
 *    ficticio vinculado a ela.
 *  - IDEMPOTENTE: se os documentos de demo ja existem, nao duplica.
 *  - Gated por app.demo.docs.enabled=true (default false) — nao roda sozinho.
 *  - Respeita a arquitetura real: object key opaco (UUID), nome do arquivo
 *    cifrado (AES-256-GCM via @Convert), CPF cifrado + cpfHash para unicidade.
 *
 * Roda em ApplicationReadyEvent (apos o contexto e o storage estarem prontos).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.demo.docs.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoDocumentosSeeder {

    private static final String FUNCIONARIO_DEMO = "Ariquemes Cunha";
    // CPF ficticio gerado pela plataforma 4dev (nao e dado real).
    private static final String CPF_DEMO = "11827545023";

    private final ContratadaRepository contratadaRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final DocumentoRepository documentoRepository;
    private final FieldEncryptionService fieldEncryptionService;
    private final MinioService minioService;

    /** Documento de empresa -> referencia a Contratada. */
    private record DemoDoc(String arquivo, TipoDocumento tipo, boolean deFuncionario, StatusDocumento status) {}

    private static final List<DemoDoc> DOCS = List.of(
            new DemoDoc("01-cartao-cnpj-empresa01.pdf",            TipoDocumento.CNPJ,          false, StatusDocumento.APROVADO),
            new DemoDoc("02-cnd-federal-empresa01.pdf",            TipoDocumento.CND_FEDERAL,   false, StatusDocumento.APROVADO),
            new DemoDoc("03-cnd-estadual-empresa01.pdf",           TipoDocumento.CND_ESTADUAL,  false, StatusDocumento.PENDENTE),
            new DemoDoc("04-cnd-municipal-empresa01.pdf",          TipoDocumento.CND_MUNICIPAL, false, StatusDocumento.REPROVADO),
            new DemoDoc("05-ordem-de-servico-empresa01.pdf",       TipoDocumento.ORDEM_SERVICO, false, StatusDocumento.APROVADO),
            new DemoDoc("06-ctps-digital-ariquemes-cunha.pdf",     TipoDocumento.CTPS_DIGITAL,  true,  StatusDocumento.APROVADO),
            new DemoDoc("07-aso-ariquemes-cunha.pdf",              TipoDocumento.ASO,           true,  StatusDocumento.PENDENTE),
            new DemoDoc("08-ficha-epi-ariquemes-cunha.pdf",        TipoDocumento.FICHA_EPI,     true,  StatusDocumento.APROVADO),
            new DemoDoc("09-holerite-ariquemes-cunha.pdf",         TipoDocumento.HOLERITE,      true,  StatusDocumento.REPROVADO),
            new DemoDoc("10-certificado-nr35-ariquemes-cunha.pdf", TipoDocumento.NR35,          true,  StatusDocumento.APROVADO)
    );

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        List<Contratada> contratadas = contratadaRepository.findAll();
        if (contratadas.isEmpty()) {
            log.warn("[demo-docs] Nenhuma contratada existente. Seeder nao executa (nao cria usuarios).");
            return;
        }
        Contratada contratada = contratadas.get(0);

        // Idempotencia: ja existem documentos de demo para esta contratada?
        long existentes = documentoRepository.count();
        if (existentes >= DOCS.size()) {
            log.info("[demo-docs] Documentos ja presentes ({}). Seeder nao duplica.", existentes);
            return;
        }

        Funcionario funcionario = obterOuCriarFuncionarioDemo(contratada);

        int criados = 0;
        for (DemoDoc d : DOCS) {
            try {
                byte[] bytes = new ClassPathResource("demo-docs/" + d.arquivo()).getInputStream().readAllBytes();
                String objectKey = UUID.randomUUID().toString();
                minioService.uploadBytes(objectKey, bytes, "application/pdf");

                Documento doc = new Documento();
                doc.setTipoDocumento(d.tipo());
                doc.setStatusDocumento(d.status());
                doc.setDataPostagem(LocalDateTime.now());
                doc.setDataStatusAtualizado(LocalDateTime.now());
                doc.setContentType("application/pdf");
                doc.setTamanhoBytes((long) bytes.length);
                doc.setObjectKey(objectKey);
                doc.setNomeArquivoOriginal(d.arquivo()); // cifrado pelo converter
                doc.setNomeArquivo("documento-demo-" + d.tipo().name().toLowerCase());
                doc.setContratada(contratada);
                if (d.deFuncionario()) {
                    doc.setFuncionario(funcionario);
                }
                documentoRepository.save(doc);
                criados++;
            } catch (Exception e) {
                log.error("[demo-docs] Falha ao semear {}: {}", d.arquivo(), e.getMessage());
            }
        }
        log.info("[demo-docs] Seed concluido: {} documentos criados para a contratada '{}'.",
                criados, contratada.getNome());
    }

    private Funcionario obterOuCriarFuncionarioDemo(Contratada contratada) {
        String cpfHash = fieldEncryptionService.hash(CPF_DEMO);
        if (funcionarioRepository.existsByCpfHashAndContratada(cpfHash, contratada)) {
            return funcionarioRepository.findByContratadaAndNomeCompletoContainingIgnoreCase(
                    contratada, FUNCIONARIO_DEMO);
        }
        Funcionario funcionario = Funcionario.builder()
                .nomeCompleto(FUNCIONARIO_DEMO)
                .cpf(CPF_DEMO)       // cifrado pelo EncryptedStringConverter
                .cpfHash(cpfHash)    // HMAC para unicidade
                .contratada(contratada)
                .build();
        return funcionarioRepository.save(funcionario);
    }
}
