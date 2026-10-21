package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.LorpenAdierazlea;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_zeharkako_ebidentzia", uniqueConstraints =
    @UniqueConstraint(name = "uk_zeharkako_ebidentzia_ordena", columnNames = {"errubrika_id", "ordena"}))
@Getter @Setter
public class ZeharkakoEbidentzia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "errubrika_id") private ZeharkakoErrubrika errubrika;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "adierazlea_id") private LorpenAdierazlea adierazlea;
    @Column(nullable = false) private Integer ordena;
    @Column(nullable = false, columnDefinition = "TEXT") private String deskribapena;
    @Column(nullable = false, precision = 5, scale = 2, columnDefinition = "decimal(5,2) default 0.00")
    private BigDecimal pisua = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, columnDefinition = "varchar(40) default 'IRAKASLEAK_SOILIK'")
    private ZeharkakoEbidentziaMota mota = ZeharkakoEbidentziaMota.IRAKASLEAK_SOILIK;
    @OneToMany(mappedBy = "ebidentzia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ZeharkakoEbidentziaMaila> mailak = new ArrayList<>();
}
