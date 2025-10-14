package com.koadernoa.app.objektuak.koadernoak.entitateak;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class KoadernoOrdutegiBlokea {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Koadernoa koadernoa;

    @Enumerated(EnumType.STRING)
    private Astegunak asteguna;  // zure enum-a erabilita

    private int hasieraSlot;     // 1..12
    private int iraupenaSlot;    // zenbat slot jarraian (>=1)

    public int bukaeraSlot() {
        return hasieraSlot + iraupenaSlot - 1;
    }
}
