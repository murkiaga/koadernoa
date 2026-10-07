package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.LorpenAdierazlea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_ebidentzia", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_ebidentzia_ordena", columnNames = {"errubrika_id", "ordena"}))
@Getter @Setter
public class ErronkaEbidentzia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ErronkaErrubrika errubrika;
    @Column(nullable = false) private Integer ordena;
    @Column(nullable = false, columnDefinition = "TEXT") private String deskribapena;
    @Column(nullable = false, precision = 5, scale = 2, columnDefinition = "decimal(5,2) default 0.00")
    private BigDecimal pisua = BigDecimal.ZERO;
    @OneToMany(mappedBy = "ebidentzia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErronkaEbidentziaMaila> mailak = new ArrayList<>();
    @OneToMany(mappedBy = "ebidentzia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErronkaEbidentziaTaldeNota> taldeNotak = new ArrayList<>();
    // Prest dago etorkizuneko ebidentzia ↔ lorpen-adierazle N:M loturarako.
    @ManyToMany @JoinTable(name = "ethazi_erronka_ebidentzia_adierazlea",
        // Keep Hibernate's original implicit column names: existing installations already have these columns.
        joinColumns = @JoinColumn(name = "erronka_ebidentzia_id"), inverseJoinColumns = @JoinColumn(name = "lorpen_adierazleak_id"),
        uniqueConstraints = @UniqueConstraint(name = "uk_erronka_ebidentzia_adierazlea", columnNames = {"erronka_ebidentzia_id", "lorpen_adierazleak_id"}))
    private Set<LorpenAdierazlea> lorpenAdierazleak = new LinkedHashSet<>();
}
