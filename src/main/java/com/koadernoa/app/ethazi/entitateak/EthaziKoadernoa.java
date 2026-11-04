package com.koadernoa.app.ethazi.entitateak;

import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "ethazi_koadernoa", uniqueConstraints = @UniqueConstraint(
        name = "uk_ethazi_koadernoa_taldea", columnNames = "taldea_id"))
public class EthaziKoadernoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "taldea_id", nullable = false)
    private Taldea taldea;

    public String getIzena() {
        String taldeKodea = taldea != null && taldea.getIzena() != null && !taldea.getIzena().isBlank()
                ? taldea.getIzena()
                : "(talderik gabe)";
        return "Ethazi koadernoa " + taldeKodea;
    }
}
