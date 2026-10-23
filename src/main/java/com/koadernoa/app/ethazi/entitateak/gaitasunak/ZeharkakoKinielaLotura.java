package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Ziklo-mailako zeharkako Kiniela; ez dauka IE edo modulu testuingururik. */
@Entity
@Table(name="ethazi_zeharkako_kiniela_lotura", uniqueConstraints=@UniqueConstraint(
    name="uk_zeharkako_kiniela", columnNames={"adierazlea_id", "gaitasuna_id", "zikloa_id"}))
@Getter @Setter
public class ZeharkakoKinielaLotura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="adierazlea_id", nullable=false)
    private LorpenAdierazlea adierazlea;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="gaitasuna_id", nullable=false)
    private Gaitasuna gaitasuna;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="zikloa_id", nullable=false)
    private Zikloa zikloa;
    @Column(nullable=false, precision=5, scale=2)
    private BigDecimal pisua = BigDecimal.ZERO;
    @Column(columnDefinition="TEXT")
    private String oharra = "";
    @ManyToMany
    @JoinTable(name="ethazi_zeharkako_kiniela_erronka",
        joinColumns=@JoinColumn(name="lotura_id"), inverseJoinColumns=@JoinColumn(name="erronka_id"),
        uniqueConstraints=@UniqueConstraint(name="uk_zeharkako_kiniela_erronka", columnNames={"lotura_id", "erronka_id"}))
    private Set<Erronka> erronkak = new LinkedHashSet<>();
}
