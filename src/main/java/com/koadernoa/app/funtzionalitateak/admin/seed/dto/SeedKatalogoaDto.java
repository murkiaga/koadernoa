package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedKatalogoaDto(
        Integer bertsioa,
        String iturria,
        String oharra,
        List<SeedMailaDto> mailak,
        List<SeedFamiliaDto> familiak) {
}
