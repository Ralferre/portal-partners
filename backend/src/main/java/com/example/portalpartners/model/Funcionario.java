package com.example.portalpartners.model;

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

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String nomeCompleto;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getNomeCompleto() {
        return nomeCompleto;
    }

    public void setNomeCompleto(String nomeCompleto) {
        this.nomeCompleto = nomeCompleto;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL)
    private List<Documento> documentos = new ArrayList<>();

    public Contratada getContratada() {
        return contratada;
    }

    public void setContratada(Contratada contratada) {
        this.contratada = contratada;
    }

    public List<Documento> getDocumentos() {
        return documentos;
    }

    public void setDocumentos(List<Documento> documentos) {
        this.documentos = documentos;
    }
}
