package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_ebidentzia_ikasle_nota", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_ebidentzia_ikasle_nota", columnNames = {"ebidentzia_id", "ikaslea_id"}))
@Getter @Setter
public class ErronkaEbidentziaIkasleNota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ebidentzia_id") private ErronkaEbidentzia ebidentzia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ikaslea_id") private Ikaslea ikaslea;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "maila_id") private ErronkaErrubrikaMaila maila;
    @Column(precision = 5, scale = 2) private BigDecimal nota;
    @Column(name = "ondo_egindakoak", columnDefinition = "TEXT") private String ondoEgindakoak;
    @Column(name = "hobetu_beharrekoak", columnDefinition = "TEXT") private String hobetuBeharrekoak;
}
