package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.math.BigDecimal;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "jarduera_kalifikazioak", uniqueConstraints = @UniqueConstraint(
        name = "uk_jarduera_kalifikazioa_jarduera_matrikula",
        columnNames = {"jarduera_id", "matrikula_id"}))
public class JardueraKalifikazioa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "jarduera_id", nullable = false)
    private Jarduera jarduera;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "matrikula_id", nullable = false)
    private Matrikula matrikula;

    @Column(nullable = false, precision = 4, scale = 2)
    private BigDecimal nota;
}
