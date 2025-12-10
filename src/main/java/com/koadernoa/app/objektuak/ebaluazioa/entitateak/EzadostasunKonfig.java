package com.koadernoa.app.objektuak.ebaluazioa.entitateak;

import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class EzadostasunKonfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifikatzaile logikoa:
     *  - "DEFAULT"
     *  - "1_EBAL", "2_EBAL"
     *  - "1_FINAL", "2_FINAL", ...
     */
    @Column(nullable = false, unique = true)
    private String kodea;

    @Column(nullable = false)
    private Integer minBlokePortzentaia = 100; // % UD emanda / UD aurreikusita

    @Column(nullable = false)
    private Integer minOrduPortzentaia = 90;   // % ordu emanda / ordu aurreikusita

    @Column(nullable = false)
    private Integer minBertaratzePortzentaia = 80; // % bertaratzea

    @Column(nullable = false)
    private Integer minGaindituPortzentaia = 50;   // % gainditu / ebaluatu
}
