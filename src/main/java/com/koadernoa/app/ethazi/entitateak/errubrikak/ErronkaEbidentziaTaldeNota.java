package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;

import com.koadernoa.app.ethazi.entitateak.ErronkaTaldea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_ebidentzia_talde_nota", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_ebidentzia_talde_nota", columnNames = {"ebidentzia_id", "taldea_id"}))
@Getter @Setter
public class ErronkaEbidentziaTaldeNota {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ebidentzia_id") private ErronkaEbidentzia ebidentzia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "taldea_id") private ErronkaTaldea taldea;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "maila_id") private ErronkaErrubrikaMaila maila;
    // Mailaren balioaren kopia, ondorengo kalkuluak eta aurreko datuen bateragarritasuna errazteko.
    @Column(precision = 5, scale = 2) private BigDecimal nota;
    @Column(name = "ondo_egindakoak", columnDefinition = "TEXT") private String ondoEgindakoak;
    @Column(name = "hobetu_beharrekoak", columnDefinition = "TEXT") private String hobetuBeharrekoak;
}
