package com.example.portalpartners.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servico de criptografia de campos sensiveis em repouso (AES-256-GCM).
 *
 * AES-256-GCM e criptografia autenticada: alem de cifrar, inclui uma tag
 * de autenticacao (GCM tag de 128 bits) que detecta qualquer adulteracao
 * do ciphertext antes da decifragem. Isso elimina vulnerabilidades como
 * bit-flipping attacks presentes em AES-CBC.
 *
 * Formato do ciphertext armazenado: Base64(IV[12 bytes] + ciphertext + GCM_TAG[16 bytes])
 *
 * Para campos que precisam de busca/unicidade (ex: CPF), use o metodo hash()
 * que gera um HMAC-SHA256 deterministico, nunca expondo o valor real.
 */
@Service
public class FieldEncryptionService {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public FieldEncryptionService(
            @Value("${app.field-encryption-key}") String base64Key
    ) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "APP_FIELD_ENCRYPTION_KEY deve ter exatamente 32 bytes (256 bits) em Base64.");
        }
        this.aesKey  = new SecretKeySpec(keyBytes, "AES");
        this.hmacKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    /**
     * Cifra um valor plaintext com AES-256-GCM.
     * Cada chamada gera um IV aleatorio de 12 bytes (nonce), garantindo
     * que o mesmo plaintext produza ciphertexts diferentes a cada execucao
     * (propriedade de segurança semantica / IND-CPA).
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Concatenar IV + ciphertext (que ja inclui a GCM tag ao final)
            byte[] combined = new byte[IV_LENGTH_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_LENGTH_BYTES, ciphertext.length);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception e) {
            throw new CryptoException("Falha ao cifrar campo sensivel", e);
        }
    }

    /**
     * Decifra um valor previamente cifrado por encrypt().
     * A GCM tag autentica o ciphertext: se o dado foi adulterado, uma
     * AEADBadTagException e lancada antes de qualquer dado ser exposto.
     *
     * Tratamento de legado: se o valor nao parece ser Base64 URL-safe ou
     * tem tamanho insuficiente, retorna o valor original sem decifrar
     * para garantir retrocompatibilidade com dados antes da criptografia.
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] combined = Base64.getUrlDecoder().decode(ciphertext);

            if (combined.length <= IV_LENGTH_BYTES) {
                return ciphertext; // dado legado nao cifrado
            }

            byte[] iv         = new byte[IV_LENGTH_BYTES];
            byte[] encrypted  = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Dado legado ou nao cifrado: retorna o valor sem decifrar
            return ciphertext;
        }
    }

    /**
     * Gera um HMAC-SHA256 deterministico do valor.
     * Usado para campos que precisam de unicidade ou busca no banco
     * (ex: CPF) sem expor o valor real. Dois valores iguais sempre
     * geram o mesmo hash, permitindo queries de existencia/unicidade.
     */
    public String hash(String value) {
        if (value == null) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            byte[] hashBytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new CryptoException("Falha ao gerar hash HMAC-SHA256", e);
        }
    }

    public static class CryptoException extends RuntimeException {
        public CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
