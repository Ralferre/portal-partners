package com.example.portalpartners.util;

import com.example.portalpartners.service.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Garante a existencia do bucket na subida da aplicacao.
 *
 * O storage pode nao estar pronto no exato momento em que a API sobe: em
 * ambientes gerenciados o servico de storage pode estar hibernado e levar
 * dezenas de segundos para responder. Por isso ha retentativas com espera
 * crescente em vez de uma unica tentativa.
 *
 * Politica de falha, apos esgotar as tentativas:
 *  - modo estrito (minio.bucket-encryption-required=true): a aplicacao NAO
 *    sobe. Producao nao pode aceitar upload sem a garantia de criptografia.
 *  - modo permissivo (false, usado em demo/homologacao): loga ERROR e deixa
 *    subir. Login e telas funcionam; upload e download falham ate o storage
 *    responder.
 */
@Slf4j
@Component
public class BucketInitializer {

    private static final int MAX_TENTATIVAS = 6;
    private static final long ESPERA_INICIAL_MS = 3000L;

    private final MinioClient minioClient;
    private final MinioService minioService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.bucket-encryption-required:true}")
    private boolean modoEstrito;

    public BucketInitializer(MinioClient minioClient, MinioService minioService) {
        this.minioClient = minioClient;
        this.minioService = minioService;
    }

    @PostConstruct
    public void createBucketIfNotExists() {
        Exception ultimaFalha = null;
        long espera = ESPERA_INICIAL_MS;

        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            try {
                inicializarBucket();
                return;
            } catch (Exception ex) {
                ultimaFalha = ex;
                log.warn("Storage indisponivel (tentativa {}/{}): {}",
                        tentativa, MAX_TENTATIVAS, ex.getMessage());

                if (tentativa < MAX_TENTATIVAS) {
                    dormir(espera);
                    espera = Math.min(espera * 2, 30000L);
                }
            }
        }

        if (modoEstrito) {
            throw new RuntimeException(
                    "Falha ao inicializar bucket MinIO apos " + MAX_TENTATIVAS
                            + " tentativas. Com minio.bucket-encryption-required=true a "
                            + "aplicacao nao sobe sem storage validado.", ultimaFalha);
        }

        log.error("Storage inacessivel apos {} tentativas. A aplicacao vai subir, mas "
                        + "upload e download de documentos FALHARAO ate o storage responder. "
                        + "Causa: {}", MAX_TENTATIVAS,
                ultimaFalha == null ? "desconhecida" : ultimaFalha.getMessage());
    }

    private void inicializarBucket() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Bucket criado: {}", bucketName);
        } else {
            log.info("Bucket ja existe: {}", bucketName);
        }

        // Configura e valida SSE no bucket. Com modo estrito habilitado,
        // a aplicacao falha na inicializacao se nao validar criptografia.
        minioService.configurarCriptografiaBucket();
    }

    private void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Inicializacao do bucket interrompida", ie);
        }
    }
}
