package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedFamiliaDto(
        String izena,
        String slug,
        Boolean aktibo,
        List<SeedZikloaDto> zikloak) {
}
