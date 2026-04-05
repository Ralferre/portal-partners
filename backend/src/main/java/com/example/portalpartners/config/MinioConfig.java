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

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(rootUser, rootPassword)
                .build();
    }

    @Bean("minioPresignClient")
    public MinioClient minioPresignClient() {
        return MinioClient.builder()
                .endpoint(minioPublicUrl)
                .credentials(rootUser, rootPassword)
                .build();
    }
}
