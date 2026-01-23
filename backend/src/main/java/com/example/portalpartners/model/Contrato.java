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

    @ManyToOne
    @JoinColumn(name = "contratante", nullable = false)
    private Contratante contratante;

    @ManyToOne
    @JoinColumn(name = "contratada_id", nullable = false)
    private Contratada contratada;

    @Column
    private String arquivoContratual;
}
