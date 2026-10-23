package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_errubrika", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_errubrika_modulua", columnNames = {"erronka_id", "moduloa_id"}))
@Getter @Setter
public class ErronkaErrubrika {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "erronka_id") private Erronka erronka;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "moduloa_id") private Moduloa moduloa;
    @Column(name = "errubrika_pisua", nullable = false, precision = 5, scale = 2,
        columnDefinition = "decimal(5,2) default 100.00") private BigDecimal errubrikaPisua = new BigDecimal("100.00");
    @Column(name = "bestelako_proben_pisua", nullable = false, precision = 5, scale = 2,
        columnDefinition = "decimal(5,2) default 0.00") private BigDecimal bestelakoProbenPisua = BigDecimal.ZERO;
    @Column(name = "gutxieneko_nota", precision = 4, scale = 2) private BigDecimal gutxienekoNota;
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC") private List<ErronkaErrubrikaMaila> mailak = new ArrayList<>();
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC") private List<ErronkaEbidentzia> ebidentziak = new ArrayList<>();
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErronkaErrubrikaTaldeOharra> taldeOharrak = new ArrayList<>();
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErronkaErrubrikaIkasleOharra> ikasleOharrak = new ArrayList<>();
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data ASC, id ASC") private List<ErronkaBestelakoProba> bestelakoProbak = new ArrayList<>();
}
