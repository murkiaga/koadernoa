package com.koadernoa.app.ethazi.service;

import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

public final class HizkuntzaTestua {
    private HizkuntzaTestua() {}
    public static String erakutsi(Hizkuntza h, String eu, String es, String en) {
        String value = h == Hizkuntza.GAZTELERA ? es : h == Hizkuntza.INGELERA ? en : eu;
        return value == null || value.isBlank() ? "[Itzulpena falta da]" : value;
    }
    public static String balidatu(String eu, String es, String en, int max, boolean required) {
        if (required && hutsik(eu) && hutsik(es) && hutsik(en))
            throw new IllegalArgumentException("Bete gutxienez hizkuntza bateko testua.");
        for (String s : new String[]{eu, es, en})
            if (s != null && s.length() > max) throw new IllegalArgumentException("Testua luzeegia da: " + max);
        return eu == null ? "" : eu.strip();
    }
    public static boolean hutsik(String s) { return s == null || s.isBlank(); }
}
