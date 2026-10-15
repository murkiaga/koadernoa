package com.koadernoa.app.objektuak.zikloak.entitateak;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.ForeignKey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Zikloa {
    public static final BigDecimal DEFEKTUZKO_ETHAZI_TEKNIKOEN_PISUA = new BigDecimal("70.00");
    public static final BigDecimal DEFEKTUZKO_ETHAZI_ZEHARKAKOEN_PISUA = new BigDecimal("30.00");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;

    @Enumerated(EnumType.STRING)
    private ZikloMaila maila; //ErdiMaila, GoiMaila
    
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_zikloa_familia"))
    private Familia familia;

    @OneToMany(mappedBy = "zikloa")
    private List<Taldea> taldeak;

    @Column(name = "ethazi_teknikoen_pisua", precision = 5, scale = 2)
    private BigDecimal ethaziTeknikoenPisua;

    @Column(name = "ethazi_zeharkakoen_pisua", precision = 5, scale = 2)
    private BigDecimal ethaziZeharkakoenPisua;

    @PrePersist
    void ezarriDefektuzkoEthaziPisuak() {
        if (ethaziTeknikoenPisua == null) {
            ethaziTeknikoenPisua = DEFEKTUZKO_ETHAZI_TEKNIKOEN_PISUA;
        }
        if (ethaziZeharkakoenPisua == null) {
            ethaziZeharkakoenPisua = DEFEKTUZKO_ETHAZI_ZEHARKAKOEN_PISUA;
        }
    }
}
