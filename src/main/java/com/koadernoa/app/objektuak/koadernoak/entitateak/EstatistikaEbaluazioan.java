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
    
    
    //%ak kalkulatzeko
    @Transient
    public Integer getUdPortzentaia() {
        if (unitateakAurreikusiak <= 0) return null;
        return (int) Math.round(100.0 * unitateakEmanda / (double) unitateakAurreikusiak);
    }

    @Transient
    public Integer getOrduPortzentaia() {
        if (orduakAurreikusiak <= 0) return null;
        return (int) Math.round(100.0 * orduakEmanda / (double) orduakAurreikusiak);
    }

    @Transient
    public Integer getGaindituPortzentaia() {
        if (ebaluatuak <= 0) return null;
        return (int) Math.round(100.0 * aprobatuak / (double) ebaluatuak);
    }

    @Transient
    public Double getBertaratzePortzentaia() {
        // 0 edo negatibo bada, ez dauka zentzurik ehunekoa kalkulatzea
        if (this.ebaluatuak <= 0 || this.orduakAurreikusiak <= 0) {
            return null;
        }

        int totalTeoriko = this.ebaluatuak * this.orduakAurreikusiak;
        if (totalTeoriko <= 0) {
            return null;
        }

        int hutsOrdu = this.hutsegiteOrduak; // int bada, ez dago null arriskurik

        // 100 * (1 - hutsOrdu / totalTeoriko)
        double raw = 100.0 * (1.0 - (double) hutsOrdu / (double) totalTeoriko);

        // 2 dezimaletara biribildu: adib. 87.234 → 87.23
        return Math.round(raw * 100.0) / 100.0;
    }

}

