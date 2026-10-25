package com.example.portalpartners;

import com.example.portalpartners.config.StorageHealthIndicator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que o indicador de saude do storage reporta UP quando o MinIO
 * (container) esta disponivel, incluindo os detalhes esperados.
 */
class StorageHealthIndicatorIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    StorageHealthIndicator storageHealthIndicator;

    @Test
    void storage_quandoDisponivel_reportaUpComDetalhes() {
        var health = storageHealthIndicator.health();
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("bucket");
        assertThat(health.getDetails()).containsEntry("tipo", "S3-compativel");
    }
}
