package com.koadernoa.app.objektuak.koadernoak.entitateak;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ordutegiak")
public class Ordutegia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "koaderno_id", nullable = false)
    private Koadernoa koadernoa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Astegunak eguna;

    /** “Saio-zenbakia” — eskolako 1..10, etab. */
    @Min(1) @Max(12)
    @Column(nullable = false)
    private int orduHasiera;

    @Min(1) @Max(12)
    @Column(nullable = false)
    private int orduAmaiera;

    public Ordutegia() {}

    public Ordutegia(Koadernoa koadernoa, Astegunak eguna, int orduHasiera, int orduAmaiera) {
        this.koadernoa = koadernoa;
        this.eguna = eguna;
        this.orduHasiera = orduHasiera;
        this.orduAmaiera = orduAmaiera;
    }

    /** Emandako tartean zenbat “ordu” (saio) sartzen diren. */
    public int getIraupenaOrdutan() {
        return Math.max(0, orduAmaiera - orduHasiera + 1);
    }

}
