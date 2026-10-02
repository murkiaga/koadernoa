package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.List;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

public record KoadernoPlangintzaKontrola(
        Koadernoa koadernoa,
        List<PlangintzaEgunKontrola> egunak,
        int planifikatutakoEgunak,
        int kontrolatuBeharrekoEgunak,
        Egoera egoera) {

    public enum Egoera {
        ONDO,
        OSATU_GABE,
        KLASE_EGUNIK_EZ
    }

    public record PlangintzaEgunKontrola(LocalDate data, boolean jardueraDu) {}

    public boolean egunean() {
        return egoera == Egoera.ONDO;
    }

    public boolean abisuaBeharDu() {
        return egoera == Egoera.OSATU_GABE;
    }

    public List<LocalDate> faltaDirenEgunak() {
        return egunak.stream()
                .filter(eguna -> !eguna.jardueraDu())
                .map(PlangintzaEgunKontrola::data)
                .toList();
    }
}
