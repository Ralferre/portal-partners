package com.example.portalpartners.model;

import com.example.portalpartners.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Funcionario de uma CONTRATADA. Usa {@code @Getter}/{@code @Setter} (e nao
 * {@code @Data}) para evitar equals/hashCode/toString sobre relacionamentos
 * LAZY (ver nota em Documento).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Funcionario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * CPF cifrado com AES-256-GCM.
     * Para busca/unicidade use cpfHash (HMAC-SHA256 deterministico).
     * A constraint unique foi removida deste campo pois ciphertexts com IV
     * aleatorio sao sempre diferentes para o mesmo plaintext.
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false)
    private String cpf;

    /**
     * HMAC-SHA256 do CPF normalizado.
     * Deterministico: o mesmo CPF sempre gera o mesmo hash com a mesma chave.
     * Permite queries de existencia/unicidade sem expor o CPF em plaintext.
     * Unique constraint garantida aqui.
     */
    @Column(name = "cpf_hash", unique = true)
    private String cpfHash;

    @Column(nullable = false)
    private String nomeCompleto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL)
    private List<Documento> documentos = new ArrayList<>();
}
