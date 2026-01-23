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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id")
    private Contratada contratada;

    @OneToMany(mappedBy = "funcionario", cascade = CascadeType.ALL)
    private List<Documento> documentos = new ArrayList<>();

}
