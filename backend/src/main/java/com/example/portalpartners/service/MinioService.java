package com.example.portalpartners.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MinioService {

    private final MinioClient client;

    @Value("${minio.bucket-name}")
    private String bucket;

    public MinioService(MinioClient client) {
        this.client = client;
    }

    public String uploadFile(MultipartFile file) {
        try {
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(file.getOriginalFilename())
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return "Uploaded: " + file.getOriginalFilename();

        } catch (Exception e) {
            throw new RuntimeException("Upload failed", e);
        }
    }
}
