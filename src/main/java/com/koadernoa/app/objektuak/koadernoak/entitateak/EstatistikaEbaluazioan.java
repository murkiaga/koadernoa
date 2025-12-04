package com.koadernoa.app.objektuak.koadernoak.entitateak;


import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class EstatistikaEbaluazioan {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private EbaluazioMomentua ebaluazioMomentua;

    private int unitateakEmanda;
    private int unitateakAurreikusiak;

    private int orduakEmanda;
    private int orduakAurreikusiak;

    private int ebaluatuak;
    private int aprobatuak;
    
    //private int ikasleKopurua; Ez da behar? ebaluatuak-ekin badakigu zenbat ikasle dauden
    private int hutsegiteOrduak;

    @ManyToOne
    private Koadernoa koadernoa;
}

