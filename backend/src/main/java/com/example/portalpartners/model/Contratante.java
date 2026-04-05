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
public class Contratante {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(unique = true)
    private String cnpj;

    @Column(unique = true)
    private String dominioEmail;

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @OneToMany(mappedBy = "contratante", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Usuario> usuarios = new ArrayList<>();

    @OneToMany(mappedBy = "contratante", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Contrato> contratos = new ArrayList<>();

    @OneToMany(mappedBy = "contratante", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Contratada> contratadas = new ArrayList<>();

}
