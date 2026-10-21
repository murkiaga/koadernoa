package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;

import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.LorpenAdierazlea;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.MailakatzeMaila;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_zeharkako_ebaluazioa", uniqueConstraints = @UniqueConstraint(
    name = "uk_zeharkako_ebaluazioa", columnNames = {"erronka_id", "moduloa_id", "adierazlea_id", "ikaslea_id"}))
@Getter @Setter
public class ZeharkakoEbaluazioa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "erronka_id") private Erronka erronka;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "moduloa_id") private Moduloa moduloa;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "adierazlea_id") private LorpenAdierazlea adierazlea;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ikaslea_id") private Ikaslea ikaslea;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "maila_id") private MailakatzeMaila maila;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal nota;
}
