package com.koadernoa.app.objektuak.ebaluazioa.entitateak;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_nota_matrikula_momentua",
    columnNames = {"matrikula_id", "ebaluazio_momentua_id"}
  )
)
public class EbaluazioNota {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Matrikula matrikula;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ebaluazio_momentua_id")
    private EbaluazioMomentua ebaluazioMomentua;

    private Double nota;     // edo BigDecimal, zuk nahiago baduzu

    private String oharra;   // “berreskuratu behar du…”
}
