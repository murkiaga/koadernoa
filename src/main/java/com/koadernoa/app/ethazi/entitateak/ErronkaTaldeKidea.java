package com.koadernoa.app.ethazi.entitateak;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_talde_kidea", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_ikaslea", columnNames = {"erronka_id", "ikaslea_id"}))
@Getter @Setter
public class ErronkaTaldeKidea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "erronka_id") private Erronka erronka;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "taldea_id") private ErronkaTaldea taldea;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ikaslea_id") private Ikaslea ikaslea;
}
