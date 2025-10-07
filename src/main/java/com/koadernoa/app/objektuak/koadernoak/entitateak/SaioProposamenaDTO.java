package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;

public class SaioProposamenaDTO {
    private LocalDate data;
    private Astegunak eguna;
    private int orduHasiera;
    private int orduAmaiera;
    private String udKodea;
    private String udIzenburua;
    private String azpiJarduera; // null izan daiteke

    public SaioProposamenaDTO(LocalDate data, Astegunak eguna, int orduHasiera, int orduAmaiera,
                           String udKodea, String udIzenburua, String azpiJarduera) {
        this.data = data;
        this.eguna = eguna;
        this.orduHasiera = orduHasiera;
        this.orduAmaiera = orduAmaiera;
        this.udKodea = udKodea;
        this.udIzenburua = udIzenburua;
        this.azpiJarduera = azpiJarduera;
    }

    // getters
    public LocalDate getData() { return data; }
    public Astegunak getEguna() { return eguna; }
    public int getOrduHasiera() { return orduHasiera; }
    public int getOrduAmaiera() { return orduAmaiera; }
    public String getUdKodea() { return udKodea; }
    public String getUdIzenburua() { return udIzenburua; }
    public String getAzpiJarduera() { return azpiJarduera; }
}
