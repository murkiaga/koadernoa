package com.koadernoa.app.objektuak.koadernoak.service;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

public record KoadernoSorreraEmaitza(Koadernoa koadernoa, Egoera egoera, String mezua) {

    public enum Egoera {
        SORTUA,
        ESLEITUA_JABE_GABEA,
        EXISTITZEN_DA
    }
}
