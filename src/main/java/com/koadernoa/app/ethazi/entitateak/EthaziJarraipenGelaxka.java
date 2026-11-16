package com.koadernoa.app.ethazi.entitateak;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "ethazi_jarraipen_gelaxka", uniqueConstraints = @UniqueConstraint(
        name = "uk_ethazi_jarraipen_gelaxka",
        columnNames = {"ethazi_koadernoa_id", "erronka_id", "ikaslea_id", "moduloa_id", "data"}),
        indexes = {
            @Index(name = "ix_ethazi_jarraipen_erronka_data", columnList = "erronka_id,data"),
            @Index(name = "ix_ethazi_jarraipen_ikaslea", columnList = "ikaslea_id")
        })
@Getter
@Setter
public class EthaziJarraipenGelaxka {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ethazi_koadernoa_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EthaziKoadernoa ethaziKoadernoa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "erronka_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Erronka erronka;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ikaslea_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Ikaslea ikaslea;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moduloa_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Moduloa moduloa;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "falta_orduak", nullable = false, precision = 6, scale = 2)
    private BigDecimal faltaOrduak = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String oharra;
}
