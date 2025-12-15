package com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FaltakBistaDTO {
    private int programaOrduak;
    private List<LocalDate> egunak;                 // zutabeen ordena
    private Map<LocalDate,Integer> egunekoOrduak;   // headerreko 2,2,3,2
    private List<FaltaIkasleRow> ikasleRows;
    // getters/setters...
}
