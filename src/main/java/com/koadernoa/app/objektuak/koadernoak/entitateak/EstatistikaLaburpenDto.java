package com.koadernoa.app.objektuak.koadernoak.entitateak;

public class EstatistikaLaburpenDto {

    private final int unitateakEmanda;
    private final int unitateakAurreikusiak;
    private final int orduakEmanda;
    private final int orduakAurreikusiak;
    private final int aprobatuak;
    private final int ebaluatuak;
    private final int bertaratzeOinarriOrduak;
    private final int hutsegiteOrduak;

    public EstatistikaLaburpenDto(int unitateakEmanda,
                                  int unitateakAurreikusiak,
                                  int orduakEmanda,
                                  int orduakAurreikusiak,
                                  int aprobatuak,
                                  int ebaluatuak,
                                  int bertaratzeOinarriOrduak,
                                  int hutsegiteOrduak) {
        this.unitateakEmanda = unitateakEmanda;
        this.unitateakAurreikusiak = unitateakAurreikusiak;
        this.orduakEmanda = orduakEmanda;
        this.orduakAurreikusiak = orduakAurreikusiak;
        this.aprobatuak = aprobatuak;
        this.ebaluatuak = ebaluatuak;
        this.bertaratzeOinarriOrduak = bertaratzeOinarriOrduak;
        this.hutsegiteOrduak = hutsegiteOrduak;
    }

    public int getUnitateakEmanda() {
        return unitateakEmanda;
    }

    public int getUnitateakAurreikusiak() {
        return unitateakAurreikusiak;
    }

    public int getOrduakEmanda() {
        return orduakEmanda;
    }

    public int getOrduakAurreikusiak() {
        return orduakAurreikusiak;
    }

    public int getAprobatuak() {
        return aprobatuak;
    }

    public int getEbaluatuak() {
        return ebaluatuak;
    }

    public int getHutsegiteOrduak() {
        return hutsegiteOrduak;
    }

    public Integer getUdPortzentaia() {
        if (unitateakAurreikusiak <= 0) return null;
        return (int) Math.round(100.0 * unitateakEmanda / (double) unitateakAurreikusiak);
    }

    public Integer getOrduPortzentaia() {
        if (orduakAurreikusiak <= 0) return null;
        return (int) Math.round(100.0 * orduakEmanda / (double) orduakAurreikusiak);
    }

    public Integer getGaindituPortzentaia() {
        if (ebaluatuak <= 0) return null;
        return (int) Math.round(100.0 * aprobatuak / (double) ebaluatuak);
    }

    public Double getBertaratzePortzentaia() {
        if (bertaratzeOinarriOrduak <= 0) {
            return null;
        }
        double raw = 100.0 * (1.0 - (double) hutsegiteOrduak / (double) bertaratzeOinarriOrduak);
        return Math.round(raw * 100.0) / 100.0;
    }
}
