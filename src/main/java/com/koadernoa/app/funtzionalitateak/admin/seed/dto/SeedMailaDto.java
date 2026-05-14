package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedMailaDto(
        String kodea,
        String izena,
        Integer ordena,
        Boolean aktibo) {
}
