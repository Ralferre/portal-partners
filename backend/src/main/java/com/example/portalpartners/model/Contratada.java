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
public class Contratada {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String cnpj;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String numeroContrato;

    @Column(nullable = false)
    private String numeroPedido;

    @OneToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "contratante_id")
    private Contratante contratante;

    @OneToMany(mappedBy = "contratada")
    private List<Contrato> contratos = new ArrayList<>();

    @OneToMany(mappedBy = "contratada")
    private List<Funcionario> funcionarios = new ArrayList<>();

}
