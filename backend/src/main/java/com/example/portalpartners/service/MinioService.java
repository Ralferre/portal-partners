package com.example.portalpartners.service;

import com.example.portalpartners.model.TipoDocumento;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient client;

    @Value("${minio.bucket-name}")
    private String bucket;

    public String uploadFile(MultipartFile file,
                             String contratadaNome,
                             String funcionarioNome,
                             TipoDocumento tipo) {
        try {
            StringBuilder prefix = new StringBuilder("contratadas/" + contratadaNome + "/documentos/" + tipo + "/");
            if (funcionarioNome != null) {
                prefix.insert(prefix.indexOf("/documentos"), "/funcionarios/" + funcionarioNome);
            }

            // Nome único do arquivo
            String extension = file.getOriginalFilename() != null
                    ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1)
                    : "bin";
            String objectName = prefix
                    .append(UUID.randomUUID())
                    .append(".")
                    .append(extension)
                    .toString();

            // Upload
            client.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return objectName;  // Retorna o path completo para salvar no banco

        } catch (Exception e) {
            throw new RuntimeException("Falha ao fazer upload para MinIO: " + e.getMessage(), e);
        }
    }

    public InputStream getObjectStream(String objectName) {
        try {
            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao baixar arquivo do MinIO: " + e.getMessage(), e);
        }
    }

    public StatObjectResponse statObject(String objectName) {
        try {
            return client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao consultar arquivo no MinIO: " + e.getMessage(), e);
        }
    }
}
