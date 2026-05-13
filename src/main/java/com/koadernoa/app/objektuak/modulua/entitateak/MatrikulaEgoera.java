package com.koadernoa.app.objektuak.modulua.entitateak;

public enum MatrikulaEgoera {
    MATRIKULATUA("MATRIKULATUA"),
    PENDIENTE_AURREKO_URTETIK("Pendiente"),
    KONBALIDATUA("Konbalidatuta"),
    DESMATRIKULATUA("DESMATRIKULATUA"),
    UKO_EGINA("UKO_EGINA"),
    GAINDITUA("Aurretik gaindituta / Konbalidatuta");

    private final String etiketa;

    MatrikulaEgoera(String etiketa) {
        this.etiketa = etiketa;
    }

    public String getEtiketa() {
        return etiketa;
    }
}
