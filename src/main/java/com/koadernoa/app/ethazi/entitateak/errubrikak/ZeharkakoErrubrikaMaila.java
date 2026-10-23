package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_zeharkako_errubrika_maila", uniqueConstraints =
    @UniqueConstraint(name = "uk_zeharkako_errubrika_maila_ordena", columnNames = {"errubrika_id", "ordena"}))
@Getter @Setter
public class ZeharkakoErrubrikaMaila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ZeharkakoErrubrika errubrika;
    @Column(nullable = false) private Integer ordena;
    @Column(nullable = false, length = 120) private String izena;
    @Column(nullable = false, precision = 4, scale = 2, columnDefinition = "decimal(4,2) default 0.00")
    private BigDecimal balioa = BigDecimal.ZERO;
    @OneToMany(mappedBy = "maila", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ZeharkakoEbidentziaMaila> ebidentziaMailak = new ArrayList<>();
}
