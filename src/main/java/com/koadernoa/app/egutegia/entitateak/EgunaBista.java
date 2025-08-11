package com.koadernoa.app.egutegia.entitateak;

import java.time.LocalDate;

//DTO sinple bat, denboralizazioko egunak hilabete aktibokoak diren ala ez jakiteko.
public class EgunaBista {
	private LocalDate data;
    private boolean hilabeteAktibokoa;

    public EgunaBista(LocalDate data, boolean hilabeteAktibokoa) {
        this.data = data;
        this.hilabeteAktibokoa = hilabeteAktibokoa;
    }

    public LocalDate getData() {
        return data;
    }

    public boolean isHilabeteAktibokoa() {
        return hilabeteAktibokoa;
    }
}
