package com.example.portalpartners.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratante_id", nullable = false)
    private Contratante contratante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratada_id", nullable = false)
    private Contratada contratada;

    @Column(nullable = false)
    private String numeroPedido;

    @Column(nullable = false)
    private String numeroContrato;

}
