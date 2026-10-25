package com.example.portalpartners;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base para testes de integracao.
 *
 * Sobe um Postgres real e um MinIO real (S3-compativel) em containers
 * efemeros, injetando as conexoes via {@link DynamicPropertySource}. Assim
 * os testes exercitam Flyway, JPA e o cliente S3 contra servicos de verdade,
 * sem depender de infraestrutura externa.
 *
 * Os containers sao estaticos e reutilizados por toda a suite (padrao
 * singleton container), reduzindo o custo de subida.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    static final GenericContainer<?> MINIO =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withCommand("server /data")
                    .withExposedPorts(9000);

    static {
        POSTGRES.start();
        MINIO.start();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        String minioUrl = "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
        registry.add("minio.url", () -> minioUrl);
        registry.add("minio.public-url", () -> minioUrl);
        registry.add("minio.root-user", () -> "minioadmin");
        registry.add("minio.root-password", () -> "minioadmin");
        registry.add("minio.access-key", () -> "minioadmin");
        registry.add("minio.secret-key", () -> "minioadmin");
    }
}
