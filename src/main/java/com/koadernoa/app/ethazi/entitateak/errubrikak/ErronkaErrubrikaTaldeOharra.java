package com.koadernoa.app.ethazi.entitateak.errubrikak;

import com.koadernoa.app.ethazi.entitateak.ErronkaTaldea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_errubrika_talde_oharra", uniqueConstraints =
    @UniqueConstraint(name = "uk_errubrika_talde_oharra", columnNames = {"errubrika_id", "taldea_id"}))
@Getter @Setter
public class ErronkaErrubrikaTaldeOharra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ErronkaErrubrika errubrika;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "taldea_id") private ErronkaTaldea taldea;
    @Column(nullable = false, columnDefinition = "TEXT") private String oharra;
}
