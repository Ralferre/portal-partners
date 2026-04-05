package com.example.portalpartners.service;

import com.example.portalpartners.audit.AuditEventDTO;
import com.example.portalpartners.audit.AuditService;
import com.example.portalpartners.dto.LgpdConsentRequest;
import com.example.portalpartners.dto.LgpdConsentResponse;
import com.example.portalpartners.dto.LgpdTermoAtualResponse;
import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.model.LgpdConsent;
import com.example.portalpartners.model.StatusAuditoria;
import com.example.portalpartners.model.Usuario;
import com.example.portalpartners.repository.LgpdConsentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LgpdService {

    private final LgpdConsentRepository lgpdConsentRepository;
    private final UsuarioLogadoService usuarioLogadoService;
    private final AuditService auditService;

    @Value("${app.lgpd.versao-termo-atual}")
    private String versaoTermoAtual;

    @Value("${app.lgpd.termo-hash-atual}")
    private String hashTermoAtual;

    @Value("${app.lgpd.termo-texto-atual}")
    private String textoTermoAtual;

    /**
     * Registra o consentimento LGPD do usuario autenticado.
     * Valida que o hashTermo recebido corresponde ao hash oficial
     * da versao corrente configurada no backend.
     */
    public LgpdConsentResponse registrarConsentimento(
            LgpdConsentRequest request,
            HttpServletRequest httpRequest
    ) {
        Usuario usuario = usuarioLogadoService.getUsuario();

        if (!versaoTermoAtual.equalsIgnoreCase(request.versaoTermo())) {
            throw new BusinessRulesException(
                    "Versao do termo invalida. Versao atual: " + versaoTermoAtual);
        }

        String hashTermoEfetivo = getHashTermoAtualEfetivo();
        if (!hashTermoEfetivo.equalsIgnoreCase(request.hashTermo())) {
            throw new BusinessRulesException(
                    "Hash do termo invalido. O conteudo do termo nao confere com a versao oficial.");
        }

        String ip        = extrairIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        LgpdConsent consent = LgpdConsent.builder()
                .userId(usuario.getId())
                .timestamp(LocalDateTime.now())
                .versaoTermo(request.versaoTermo())
                .hashTermo(hashTermoEfetivo)
                .ip(ip)
                .userAgent(userAgent)
                .build();

        LgpdConsent saved = lgpdConsentRepository.save(consent);

        auditService.registrar(AuditEventDTO.builder()
                .acao("LGPD_CONSENTIMENTO_REGISTRADO")
                .entidade("LgpdConsent")
                .entidadeId(saved.getId().toString())
                .email(usuario.getEmail())
                .userId(usuario.getId())
                .role(usuario.getRole() != null ? usuario.getRole().name() : null)
                .ip(ip)
                .userAgent(userAgent)
                .status(StatusAuditoria.SUCCESS)
                .detalhes(Map.of("versaoTermo", request.versaoTermo()))
                .build());

        return new LgpdConsentResponse(true, saved.getVersaoTermo(), saved.getTimestamp());
    }

    /**
     * Verifica se o usuario autenticado ja possui consentimento valido
     * para a versao corrente do termo LGPD.
     */
    public LgpdConsentResponse verificarConsentimentoValido() {
        Usuario usuario = usuarioLogadoService.getUsuario();

        Optional<LgpdConsent> consent = lgpdConsentRepository
                .findTopByUserIdAndVersaoTermoOrderByTimestampDesc(
                        usuario.getId(), versaoTermoAtual);

        return consent
                .map(c -> new LgpdConsentResponse(true, c.getVersaoTermo(), c.getTimestamp()))
                .orElse(new LgpdConsentResponse(false, versaoTermoAtual, null));
    }

    public LgpdTermoAtualResponse obterTermoAtual() {
        LgpdConsentResponse consentimento = verificarConsentimentoValido();
        return new LgpdTermoAtualResponse(
                consentimento.valido(),
                versaoTermoAtual,
                getHashTermoAtualEfetivo(),
                textoTermoAtual,
                consentimento.timestamp()
        );
    }

    /**
     * Valida que o usuario logado possui consentimento LGPD vigente.
     * Chamado pelo DocumentoService antes de qualquer operacao de upload.
     */
    public void exigirConsentimentoValido() {
        LgpdConsentResponse resp = verificarConsentimentoValido();
        if (!resp.valido()) {
            throw new BusinessRulesException(
                    "Consentimento LGPD necessario. Aceite os termos antes de fazer upload.");
        }
    }

    public boolean exigeConsentimentoParaDocumentosPessoais(Enum<?> tipoReferencia) {
        return tipoReferencia != null && "FUNCIONARIO".equalsIgnoreCase(tipoReferencia.name());
    }

    private String getHashTermoAtualEfetivo() {
        if (textoTermoAtual != null && !textoTermoAtual.isBlank()) {
            return sha256(textoTermoAtual);
        }
        return hashTermoAtual;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new BusinessRulesException("Nao foi possivel calcular o hash do termo LGPD.");
        }
    }

    private String extrairIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
