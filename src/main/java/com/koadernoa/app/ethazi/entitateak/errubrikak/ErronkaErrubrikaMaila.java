package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_errubrika_maila", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_errubrika_maila_ordena", columnNames = {"errubrika_id", "ordena"}))
@Getter @Setter
public class ErronkaErrubrikaMaila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ErronkaErrubrika errubrika;
    @Column(nullable = false) private Integer ordena;
    @Column(nullable = false, length = 100) private String izena;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal balioa;
    @OneToMany(mappedBy = "maila", cascade = CascadeType.ALL)
    private List<ErronkaEbidentziaMaila> ebidentziaMailak = new ArrayList<>();
}
