package com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaltaIkasleRow {
	private Matrikula matrikula;

    private int faltaOrduak;         // guztira (ikasturte osoa + orain arte)
    private double faltaPortzentaia; // dto-ak kalkulatzen du

    // Hilabete honetako eguna -> etiketa (2, 3, b ...)
    private Map<LocalDate,String> balioak = new HashMap<>();

    public String getIkasleIzenOsoa() {
        if (matrikula == null || matrikula.getIkaslea() == null) return "";
        var i = matrikula.getIkaslea();
        return (i.getAbizena1() + " " +
                (i.getAbizena2() != null ? i.getAbizena2() + " " : "") +
                i.getIzena()).trim();
    }

    /**
     * orduak → beti gehitzen da (totala egiteko)
     * etiketa → hilabete honetako egunetarako bakarrik (bestela null)
     */
    public void gehituFalta(LocalDate data, int orduak, String etiketa) {
        this.faltaOrduak += orduak;

        if (etiketa == null) {
            return; // aurreko hilabeteetako faltak: totala bai, zelula ez
        }

        balioak.merge(data, etiketa, (oldVal, newVal) -> {
            // Bi balioak zenbakiak badira, batu (egun berean bi saio)
            if (oldVal.matches("\\d+") && newVal.matches("\\d+")) {
                int n1 = Integer.parseInt(oldVal);
                int n2 = Integer.parseInt(newVal);
                return String.valueOf(n1 + n2);
            }
            // bestela, kateatu
            return oldVal + "," + newVal;
        });
    }

    public String balioa(LocalDate eguna) {
        return balioak.getOrDefault(eguna, "");
    }
}