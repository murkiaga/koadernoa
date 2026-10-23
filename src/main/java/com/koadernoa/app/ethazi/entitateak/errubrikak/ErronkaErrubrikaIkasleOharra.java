package com.koadernoa.app.ethazi.entitateak.errubrikak;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_errubrika_ikasle_oharra", uniqueConstraints =
    @UniqueConstraint(name = "uk_errubrika_ikasle_oharra", columnNames = {"errubrika_id", "ikaslea_id"}))
@Getter @Setter
public class ErronkaErrubrikaIkasleOharra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ErronkaErrubrika errubrika;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ikaslea_id") private Ikaslea ikaslea;
    @Column(nullable = false, columnDefinition = "TEXT") private String oharra;
}
