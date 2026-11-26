package com.koadernoa.app.ethazi.entitateak;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.koadernoa.app.ethazi.entitateak.kronograma.ErronkaKronogramaJarduera;
import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_denboralizazio_jarduera", indexes = {
        @Index(name = "ix_ethazi_denb_koaderno_data", columnList = "ethazi_koadernoa_id,data"),
        @Index(name = "ix_ethazi_denb_erronka", columnList = "ethazi_koadernoa_id,erronka_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_ethazi_denb_jatorria_data",
        columnNames = {"ethazi_koadernoa_id", "jatorrizko_jarduera_id", "data"}))
@Getter @Setter
public class EthaziDenboralizazioJarduera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ethazi_koadernoa_id", nullable = false)
    private EthaziKoadernoa ethaziKoadernoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "erronka_id", nullable = false)
    private Erronka erronka;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jatorrizko_jarduera_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private ErronkaKronogramaJarduera jatorrizkoJarduera;

    @Column(name = "erronka_izena", nullable = false, length = 200)
    private String erronkaIzena;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false, length = 200)
    private String izenburua;

    @Column(columnDefinition = "TEXT")
    private String deskribapena;

    @Column(columnDefinition = "TEXT")
    private String oharrak;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KronogramaJardueraMota mota;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moduloa_id")
    private Moduloa moduloa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aurreikusitako_moduloa_id")
    private Moduloa aurreikusitakoModuloa;

    @Column(nullable = false, precision = 7, scale = 2)
    private BigDecimal orduak;
}
