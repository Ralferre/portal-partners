package com.example.portalpartners.config;

import com.example.portalpartners.service.MinioService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Indicador de saude do storage de documentos (MinIO em dev, Cloudflare R2 em
 * producao — ambos S3-compativeis).
 *
 * Registrado com o nome "storage" (/actuator/health -> components.storage).
 * Fica FORA do grupo que o provedor de deploy consulta: a indisponibilidade do
 * storage (ex: cold start) nao deve derrubar o health que mantem a instancia
 * viva. Serve para observabilidade e para o painel administrativo.
 */
@Component("storage")
@RequiredArgsConstructor
public class StorageHealthIndicator implements HealthIndicator {

    private final MinioService minioService;

    @Override
    public Health health() {
        boolean up = minioService.storageDisponivel();
        if (up) {
            return Health.up()
                    .withDetail("bucket", minioService.getBucketName())
                    .withDetail("tipo", "S3-compativel")
                    .build();
        }
        return Health.down()
                .withDetail("bucket", minioService.getBucketName())
                .withDetail("motivo", "storage nao respondeu a verificacao de bucket")
                .build();
    }
}
