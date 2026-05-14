package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

public record SeedImportResult(
        int sortutakoMailak,
        int sortutakoFamiliak,
        int sortutakoZikloak,
        int sortutakoTaldeak,
        int sortutakoModuluak,
        int lehendikZeudenak) {

    public String successMessage() {
        return "Seed inportazioa amaituta: "
                + sortutakoFamiliak + " familia sortu dira, "
                + sortutakoZikloak + " ziklo sortu dira, "
                + sortutakoTaldeak + " talde sortu dira, "
                + sortutakoModuluak + " modulu sortu dira, "
                + lehendikZeudenak + " elementu lehendik zeuden.";
    }
}
