package com.example.portalpartners.service;

import com.example.portalpartners.model.TipoDocumento;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.SseAlgorithm;
import io.minio.messages.SseConfiguration;
import io.minio.messages.SseConfigurationRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {

    private final MinioClient client;
    private final MinioClient presignClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    public MinioService(
            @Qualifier("minioClient") MinioClient client,
            @Qualifier("minioPresignClient") MinioClient presignClient) {
        this.client = client;
        this.presignClient = presignClient;
    }

    // -------------------------------------------------------------------------
    // Upload legado (mantido para retrocompatibilidade - bytes passam pelo backend)
    // Para novos uploads, usar gerarPresignedPutUrl (arquitetura Zero-Copy).
    // -------------------------------------------------------------------------

    public String uploadFile(MultipartFile file,
                             String contratadaNome,
                             String funcionarioNome,
                             TipoDocumento tipo) {
        try {
            StringBuilder prefix = new StringBuilder(
                    "contratadas/" + contratadaNome + "/documentos/" + tipo + "/");
            if (funcionarioNome != null) {
                prefix.insert(prefix.indexOf("/documentos"),
                        "/funcionarios/" + funcionarioNome);
            }

            String extension = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                          .substring(file.getOriginalFilename().lastIndexOf(".") + 1)
                    : "bin";

            String objectName = prefix
                    .append(java.util.UUID.randomUUID())
                    .append(".")
                    .append(extension)
                    .toString();

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build();

            client.putObject(args);
            return objectName;

        } catch (Exception e) {
            throw new RuntimeException("Falha ao fazer upload para MinIO: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Arquitetura Zero-Copy: presigned URLs
    // Os bytes do arquivo NUNCA passam pelo processo Java do backend.
    // -------------------------------------------------------------------------

    /**
     * Gera uma presigned PUT URL para upload direto do cliente ao MinIO.
     *
     * O objectKey deve ser um UUID v4 opaco gerado pelo backend (nunca o nome real
     * do arquivo), garantindo que o storage nao exponha informacao semantica.
     *
     * TTL de 10 minutos: suficiente para uploads de ate 10MB em conexoes lentas.
     */
    public String gerarPresignedPutUrl(String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(10, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar presigned PUT URL: " + e.getMessage(), e);
        }
    }

    /**
     * Gera uma presigned GET URL para download direto do MinIO pelo cliente.
     *
     * TTL de 15 minutos: URL de uso unico; o frontend deve armazena-la apenas
     * em memoria (nunca em localStorage) e usar imediatamente.
     */
    public String gerarPresignedGetUrl(String objectKey) {
        try {
            return presignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(15, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar presigned GET URL: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // Operacoes de baixo nivel (mantidas para uso interno)
    // -------------------------------------------------------------------------

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

    /**
     * Tenta configurar SSE-S3 (criptografia padrao) no bucket.
     * Requer MinIO com KMS configurado para SSE-KMS (producao).
     * Para SSE-S3 sem KMS, falha silenciosamente e loga aviso.
     * Em producao com KES/Vault, esta chamada habilitara envelope encryption.
     */
    public void configurarCriptografiaBucket() {
        try {
            // API correta para MinIO SDK 8.6.0: construtor direto com SseConfigurationRule
            SseConfiguration config = new SseConfiguration(
                    new SseConfigurationRule(SseAlgorithm.AES256, null)
            );
            client.setBucketEncryption(
                    SetBucketEncryptionArgs.builder()
                            .bucket(bucket)
                            .config(config)
                            .build()
            );
            log.info("SSE-S3 configurado com sucesso no bucket '{}'", bucket);
        } catch (Exception e) {
            log.warn("Nao foi possivel configurar SSE no bucket '{}' (requer KMS em producao): {}",
                    bucket, e.getMessage());
        }
    }

}
