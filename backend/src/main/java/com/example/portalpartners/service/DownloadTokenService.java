package com.example.portalpartners.service;

import com.example.portalpartners.exceptions.BusinessRulesException;
import com.example.portalpartners.model.DownloadToken;
import com.example.portalpartners.repository.DownloadTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * Emite e consome autorizacoes de uso unico para download de documentos.
 *
 * Trocamos uma presigned URL de 15 minutos, reutilizavel por qualquer um que
 * a obtivesse, por um link que morre no primeiro uso e expira em 60 segundos.
 * Um link vazado depois do download nao serve para nada.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadTokenService {

    /** Janela curta: o link e usado imediatamente apos ser gerado. */
    private static final int VALIDADE_SEGUNDOS = 60;

    /** 256 bits — inviavel de adivinhar por forca bruta. */
    private static final int TOKEN_BYTES = 32;

    private final DownloadTokenRepository downloadTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public DownloadToken emitir(Long documentoId,
                                Long usuarioId,
                                String objectKey,
                                boolean marcaDownloadContratante) {
        removerExpirados();

        LocalDateTime agora = LocalDateTime.now();

        DownloadToken token = DownloadToken.builder()
                .token(gerarToken())
                .documentoId(documentoId)
                .usuarioId(usuarioId)
                .objectKey(objectKey)
                .marcaDownloadContratante(marcaDownloadContratante)
                .criadoEm(agora)
                .expiraEm(agora.plusSeconds(VALIDADE_SEGUNDOS))
                .build();

        return downloadTokenRepository.save(token);
    }

    /**
     * Marca o token como usado e devolve os dados para servir o arquivo.
     *
     * A exclusividade e garantida pelo UPDATE condicional no repositorio, e
     * nao por leitura seguida de escrita: so uma requisicao concorrente vence.
     */
    @Transactional
    public DownloadToken consumir(String token, String ip) {
        int afetados = downloadTokenRepository.consumir(token, LocalDateTime.now(), ip);

        if (afetados == 0) {
            // Nao distinguimos inexistente, expirado e ja usado de proposito:
            // a mensagem generica evita confirmar a existencia de um token.
            throw new BusinessRulesException(
                    "Link de download invalido, expirado ou ja utilizado. "
                            + "Solicite o download novamente.");
        }

        return downloadTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessRulesException("Link de download invalido"));
    }

    public int validadeSegundos() {
        return VALIDADE_SEGUNDOS;
    }

    private String gerarToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Higiene da tabela: tokens vencidos ha mais de um dia nao servem a nada. */
    private void removerExpirados() {
        try {
            int removidos = downloadTokenRepository.removerExpirados(
                    LocalDateTime.now().minusDays(1));
            if (removidos > 0) {
                log.debug("Tokens de download expirados removidos: {}", removidos);
            }
        } catch (Exception e) {
            log.warn("Falha ao limpar tokens de download expirados: {}", e.getMessage());
        }
    }
}
