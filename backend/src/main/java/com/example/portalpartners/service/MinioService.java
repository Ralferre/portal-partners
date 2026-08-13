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
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioService {

    private final MinioClient client;
    private final MinioClient presignClient;

    @Value("${minio.bucket-name}")
    private String bucket;

    @Value("${minio.bucket-encryption-required:true}")
    private boolean bucketEncryptionRequired;

    /** Uma vez confirmado, nao ha nova ida a rede pelo resto da vida da instancia. */
    private volatile boolean bucketConfirmado = false;

    private static final int TENTATIVAS_BUCKET = 3;

    public MinioService(
            @Qualifier("minioClient") MinioClient client,
            @Qualifier("minioPresignClient") MinioClient presignClient) {
        this.client = client;
        this.presignClient = presignClient;
    }

    /**
     * Garante a existencia do bucket no momento do uso, e nao apenas na subida
     * da aplicacao.
     *
     * O BucketInitializer roda uma unica vez, no startup. Se o storage estiver
     * indisponivel naquele instante (hibernado, por exemplo), a aplicacao sobe
     * sem bucket e todo upload posterior falha, mesmo depois que o storage
     * volta. Esta verificacao remove essa dependencia de ordem de inicializacao.
     *
     * O resultado positivo fica em cache: o custo e uma unica chamada por
     * instancia, na primeira vez que alguem envia um documento.
     */
    public void garantirBucketExiste() {
        if (bucketConfirmado) {
            return;
        }

        synchronized (this) {
            if (bucketConfirmado) {
                return;
            }

            Exception ultimaFalha = null;

            for (int tentativa = 1; tentativa <= TENTATIVAS_BUCKET; tentativa++) {
                try {
                    boolean existe = client.bucketExists(
                            BucketExistsArgs.builder().bucket(bucket).build());

                    if (!existe) {
                        client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                        log.info("Bucket criado sob demanda: {}", bucket);
                    }

                    bucketConfirmado = true;
                    return;
                } catch (Exception e) {
                    ultimaFalha = e;
                    log.warn("Storage nao respondeu ao verificar o bucket (tentativa {}/{}): {}",
                            tentativa, TENTATIVAS_BUCKET, e.getMessage());

                    if (tentativa < TENTATIVAS_BUCKET) {
                        dormir(4000L * tentativa);
                    }
                }
            }

            throw new RuntimeException(
                    "O storage de documentos esta indisponivel no momento. "
                            + "Aguarde cerca de um minuto e tente novamente. Detalhe tecnico: "
                            + (ultimaFalha == null ? "desconhecido" : ultimaFalha.getMessage()),
                    ultimaFalha);
        }
    }

    private void dormir(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
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
            // Legacy upload agora tambem usa object key opaca para evitar exposicao
            // de dados sensiveis (nomes de empresa/funcionario) no caminho do storage.
            String objectName = gerarObjectKeyOpacaLegado(file.getOriginalFilename());

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

    private String gerarObjectKeyOpacaLegado(String originalFilename) {
        String extension = "bin";
        if (originalFilename != null) {
            int lastDot = originalFilename.lastIndexOf('.');
            if (lastDot >= 0 && lastDot < originalFilename.length() - 1) {
                String candidate = originalFilename.substring(lastDot + 1)
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z0-9]", "");
                if (!candidate.isBlank() && candidate.length() <= 10) {
                    extension = candidate;
                }
            }
        }
        return "legacy/" + java.util.UUID.randomUUID() + "." + extension;
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
        // Sem esta garantia, um bucket ausente so aparece la na frente: a URL e
        // assinada normalmente (assinatura e local) e o navegador recebe um 404
        // opaco do storage. Verificar aqui falha cedo e com mensagem util.
        garantirBucketExiste();

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
     * Configura e valida criptografia em repouso no bucket.
     *
     * Com minio.bucket-encryption-required=true, a aplicacao entra em fail-fast
     * se nao conseguir garantir SSE no bucket.
     */
    public void configurarCriptografiaBucket() {
        Exception ultimaFalha = null;
        try {
            SseConfiguration config = new SseConfiguration(
                    new SseConfigurationRule(SseAlgorithm.AES256, null)
            );
            client.setBucketEncryption(
                    SetBucketEncryptionArgs.builder()
                            .bucket(bucket)
                            .config(config)
                            .build()
            );
            log.info("SSE-S3 configurado no bucket '{}'", bucket);
        } catch (Exception e) {
            ultimaFalha = e;
            log.warn("Falha ao configurar SSE no bucket '{}': {}",
                    bucket, e.getMessage());
        }

        try {
            client.getBucketEncryption(
                    GetBucketEncryptionArgs.builder()
                            .bucket(bucket)
                            .build()
            );
            log.info("Criptografia em repouso validada no bucket '{}'", bucket);
            return;
        } catch (Exception e) {
            if (ultimaFalha == null) {
                ultimaFalha = e;
            }
            log.warn("Falha ao validar SSE no bucket '{}': {}", bucket, e.getMessage());
        }

        if (bucketEncryptionRequired) {
            throw new IllegalStateException(
                    "Criptografia em repouso obrigatoria no bucket MinIO e nao foi validada.",
                    ultimaFalha
            );
        }

        log.warn("Criptografia em repouso NAO validada no bucket '{}'. "
                        + "Permitido apenas porque minio.bucket-encryption-required=false.",
                bucket);
    }

}
