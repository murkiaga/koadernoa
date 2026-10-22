package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Erronka osoaren zeharkako errubrika bakarra; ez dago modulu bati lotuta. */
@Entity
@Table(name = "ethazi_zeharkako_errubrika", uniqueConstraints =
    @UniqueConstraint(name = "uk_zeharkako_errubrika_erronka", columnNames = "erronka_id"))
@Getter @Setter
public class ZeharkakoErrubrika {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "erronka_id") private Erronka erronka;
    @Column(nullable = false, precision = 5, scale = 2, columnDefinition = "decimal(5,2) default 85.00")
    private BigDecimal heteroebaluazioPisua = new BigDecimal("85.00");
    @Column(nullable = false, precision = 5, scale = 2, columnDefinition = "decimal(5,2) default 10.00")
    private BigDecimal koebaluazioPisua = new BigDecimal("10.00");
    @Column(nullable = false, precision = 5, scale = 2, columnDefinition = "decimal(5,2) default 5.00")
    private BigDecimal autoebaluazioPisua = new BigDecimal("5.00");
    @Column(name = "gutxieneko_nota", precision = 4, scale = 2) private BigDecimal gutxienekoNota;
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC") private List<ZeharkakoErrubrikaMaila> mailak = new ArrayList<>();
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC") private List<ZeharkakoEbidentzia> ebidentziak = new ArrayList<>();
}
