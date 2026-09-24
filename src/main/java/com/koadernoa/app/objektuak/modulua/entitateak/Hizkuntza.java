package com.koadernoa.app.objektuak.modulua.entitateak;
public enum Hizkuntza {
    ZEHAZTU_GABE(""), EUSKARA("Euskara"), GAZTELERA("Gaztelera"), INGELERA("Ingelera");
    private final String etiketa;
    Hizkuntza(String etiketa) { this.etiketa = etiketa; }
    public String getEtiketa() { return etiketa; }
}
