package com.example.portalpartners.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.root-user}")
    private String rootUser;

    @Value("${minio.root-password}")
    private String rootPassword;

    @Value("${minio.public-url:${minio.url}}")
    private String minioPublicUrl;

    /**
     * Region e opcional e fica vazia no MinIO local. Provedores S3-compativeis
     * exigem valor explicito: "auto" no Cloudflare R2, a region real na AWS S3
     * (ex: sa-east-1). Sem isso o cliente tenta descobrir a region sozinho e a
     * assinatura da presigned URL pode sair invalida.
     */
    @Value("${minio.region:}")
    private String region;

    @Bean
    public MinioClient minioClient() {
        return build(minioUrl);
    }

    @Bean("minioPresignClient")
    public MinioClient minioPresignClient() {
        return build(minioPublicUrl);
    }

    private MinioClient build(String endpoint) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(rootUser, rootPassword);

        if (region != null && !region.isBlank()) {
            builder.region(region.trim());
        }

        return builder.build();
    }
}
