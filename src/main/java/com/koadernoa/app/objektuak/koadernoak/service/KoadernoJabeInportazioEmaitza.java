package com.koadernoa.app.objektuak.koadernoak.service;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KoadernoJabeInportazioEmaitza {
    private int prozesatutakoLerroak;
    private int esleitutakoKoadernoak;
    private final List<String> erroreak = new ArrayList<>();
    private final List<String> oharrak = new ArrayList<>();

    public void gehituErrorea(String errorea) {
        erroreak.add(errorea);
    }

    public void gehituOharra(String oharra) {
        oharrak.add(oharra);
    }

    public boolean hasErroreak() {
        return !erroreak.isEmpty();
    }

    public String laburpena() {
        return esleitutakoKoadernoak + " koaderno esleitu dira. " + erroreak.size() + " lerrok erroreak izan dituzte.";
    }

    public String erroreLaburpena() {
        int muga = Math.min(erroreak.size(), 5);
        String testua = String.join(" | ", erroreak.subList(0, muga));
        if (erroreak.size() > muga) {
            testua += " | ... (guztira " + erroreak.size() + " errore)";
        }
        return testua;
    }
}
