package com.koadernoa.app.objektuak.ordutegiak.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Getter;

@Getter
public class OrdutegiImportResult {
    private int solucfKopurua;
    private int sortutakoLerroKopurua;
    private final List<SaltatutakoLerroa> saltatutakoLerroak = new ArrayList<>();
    private final Set<String> lotuGabekoIrakasleak = new LinkedHashSet<>();
    private final Set<String> lotuGabekoTaldeak = new LinkedHashSet<>();

    public int getSaltatutakoKopurua() {
        return saltatutakoLerroak.size();
    }

    void gehituSolucf() {
        solucfKopurua++;
    }

    void gehituSortutakoLerroa() {
        sortutakoLerroKopurua++;
    }

    void gehituSaltatutakoa(SaltatutakoLerroa lerroa) {
        saltatutakoLerroak.add(lerroa);
    }

    void gehituLotuGabekoIrakaslea(String kodea, String izena, String emaila) {
        String emailTestua = emaila == null || emaila.isBlank() ? "emailik gabe" : emaila;
        lotuGabekoIrakasleak.add(kodea + " - " + (izena == null || izena.isBlank() ? "(izenik gabe)" : izena)
                + " (" + emailTestua + ") ez dago aplikazioko irakasle batekin lotuta");
    }

    void gehituLotuGabekoTaldea(String kodea) {
        lotuGabekoTaldeak.add(kodea);
    }

    public record SaltatutakoLerroa(String prof, String asig, String aula, String codgrupo,
                                    Integer dia, Integer hora, String arrazoia) {
    }
}
