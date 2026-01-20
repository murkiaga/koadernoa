package com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;

public class EzadostasunFitxaRow {
    private final EzadostasunFitxa fitxa;
    private final String ezadostasunMota;

    public EzadostasunFitxaRow(EzadostasunFitxa fitxa, String ezadostasunMota) {
        this.fitxa = fitxa;
        this.ezadostasunMota = ezadostasunMota;
    }

    public EzadostasunFitxa getFitxa() {
        return fitxa;
    }

    public String getEzadostasunMota() {
        return ezadostasunMota;
    }
}
