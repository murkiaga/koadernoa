package com.koadernoa.app.ethazi.dto;

import java.io.Serializable;

import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

public record IkaskuntzaEmaitzaImportRow(
        String eeiKodea,
        Integer ordena,
        String kodea,
        String deskribapena,
        Hizkuntza hizkuntza,
        Egoera egoera,
        String mezua,
        String dbBalioa) implements Serializable {

    public enum Egoera { SORTU, EGUNERATU, ALDAKETARIK_EZ, ARAZOA }

    public IkaskuntzaEmaitzaImportRow parsed(String message) {
        return new IkaskuntzaEmaitzaImportRow(eeiKodea, ordena, kodea, deskribapena,
                hizkuntza, null, message, null);
    }

    public IkaskuntzaEmaitzaImportRow preview(Egoera status, String message, String currentValue) {
        return new IkaskuntzaEmaitzaImportRow(eeiKodea, ordena, kodea, deskribapena,
                hizkuntza, status, message, currentValue);
    }
}
