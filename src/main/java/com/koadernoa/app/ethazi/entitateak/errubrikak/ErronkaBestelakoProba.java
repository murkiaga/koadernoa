package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.koadernoa.app.ethazi.entitateak.gaitasunak.LorpenAdierazlea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_bestelako_proba")
@Getter @Setter
public class ErronkaBestelakoProba {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ErronkaErrubrika errubrika;
    @Column(nullable = false, length = 200) private String izena;
    @Column(nullable = false) private LocalDate data;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal pisua;
    @ManyToMany
    @JoinTable(name = "ethazi_erronka_bestelako_proba_adierazlea",
        joinColumns = @JoinColumn(name = "proba_id"), inverseJoinColumns = @JoinColumn(name = "adierazlea_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_bestelako_proba_adierazlea", columnNames = {"proba_id", "adierazlea_id"}))
    private Set<LorpenAdierazlea> lorpenAdierazleak = new LinkedHashSet<>();
    @OneToMany(mappedBy = "proba", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErronkaBestelakoProbaNota> notak = new ArrayList<>();
}
