package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_bestelako_proba_nota", uniqueConstraints =
    @UniqueConstraint(name = "uk_bestelako_proba_ikasle_nota", columnNames = {"proba_id", "ikaslea_id"}))
@Getter @Setter
public class ErronkaBestelakoProbaNota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "proba_id") private ErronkaBestelakoProba proba;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ikaslea_id") private Ikaslea ikaslea;
    @Column(nullable = false, precision = 4, scale = 2) private BigDecimal nota;
}
