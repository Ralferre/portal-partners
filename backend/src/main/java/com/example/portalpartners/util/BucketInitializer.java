package com.example.portalpartners.util;

import com.example.portalpartners.service.MinioService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BucketInitializer {

    private final MinioClient minioClient;
    private final MinioService minioService;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public BucketInitializer(MinioClient minioClient, MinioService minioService) {
        this.minioClient = minioClient;
        this.minioService = minioService;
    }

    @PostConstruct
    public void createBucketIfNotExists() {
        try {
            Thread.sleep(5000);
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

            // Tenta habilitar SSE-S3. Em producao com KES/Vault, habilitara
            // envelope encryption (SSE-KMS). Em dev, loga aviso e continua.
            minioService.configurarCriptografiaBucket();

        } catch (Exception ex) {
            throw new RuntimeException("Falha ao inicializar bucket MinIO", ex);
        }
    }
}
