package com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;

public class EzadostasunFitxaRow {
    private final EzadostasunFitxa fitxa;
    private final String ezadostasunMotak;

    public EzadostasunFitxaRow(EzadostasunFitxa fitxa, String ezadostasunMotak) {
        this.fitxa = fitxa;
        this.ezadostasunMotak = ezadostasunMotak;
    }

    public EzadostasunFitxa getFitxa() {
        return fitxa;
    }

    public String getEzadostasunMotak() {
        return ezadostasunMotak;
    }
}
