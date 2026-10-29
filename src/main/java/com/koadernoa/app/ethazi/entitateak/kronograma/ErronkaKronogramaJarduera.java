package com.koadernoa.app.ethazi.entitateak.kronograma;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.koadernoa.app.ethazi.entitateak.Erronka;
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
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_kronograma_jarduera", indexes = {
    @Index(name = "ix_kronograma_jarduera_erronka_data", columnList = "erronka_id,hasiera_data,bukaera_data"),
    @Index(name = "ix_kronograma_jarduera_moduloa", columnList = "moduloa_id")
})
@Getter @Setter
public class ErronkaKronogramaJarduera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "erronka_id", nullable = false)
    private Erronka erronka;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KronogramaJardueraMota mota;

    /** MODULUA motako jardueraren jabea. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moduloa_id")
    private Moduloa moduloa;

    /** Jarduera komuna zein moduluren ordutegian egitea aurreikusten den, aukeran. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aurreikusitako_moduloa_id")
    private Moduloa aurreikusitakoModuloa;

    @Column(nullable = false, length = 200)
    private String izenburua;

    @Column(columnDefinition = "TEXT")
    private String deskribapena;

    @Column(name = "hasiera_data", nullable = false)
    private LocalDate hasieraData;

    @Column(name = "bukaera_data", nullable = false)
    private LocalDate bukaeraData;

    @Column(name = "iraupen_orduak", nullable = false, precision = 7, scale = 2)
    private BigDecimal iraupenOrduak;
}
