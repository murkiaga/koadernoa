package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedModuloaDto(
        String eeiKodea,
        String izena,
        String kodea,
        String mailaKodea,
        Integer orduak,
        Integer dualOrduak,
        Boolean hautazkoa) {
}
