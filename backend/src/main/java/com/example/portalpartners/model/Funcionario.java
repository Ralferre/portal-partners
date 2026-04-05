package com.example.portalpartners.model;

import com.example.portalpartners.crypto.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getCpfHash() { return cpfHash; }
    public void setCpfHash(String cpfHash) { this.cpfHash = cpfHash; }

    public String getNomeCompleto() { return nomeCompleto; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL)
    private List<Documento> documentos = new ArrayList<>();

    public Contratada getContratada() { return contratada; }
    public void setContratada(Contratada contratada) { this.contratada = contratada; }

    public List<Documento> getDocumentos() { return documentos; }
    public void setDocumentos(List<Documento> documentos) { this.documentos = documentos; }
}
