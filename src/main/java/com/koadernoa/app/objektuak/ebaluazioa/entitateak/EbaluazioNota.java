package com.koadernoa.app.objektuak.ebaluazioa.entitateak;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;

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

    /**
     * Nota zenbakizkoa (0-10). 
     * Egoera berezia dagoenean, normalean NULL egongo da.
     */
    private Double nota;

    /**
     * Egoera berezia: EZ_AURKEZTUA, EZ_EBAL_FALTA...
     * NULL → nota arrunta (nota zenbakizkoa).
     */
    @ManyToOne
    @JoinColumn(name = "egoera_id")
    private EbaluazioEgoera egoera;


    private String oharra;   // “berreskuratu behar du…”
}
