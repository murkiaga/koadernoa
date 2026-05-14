package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeedZikloaDto(
        String izena,
        String maila,
        List<SeedTaldeaDto> taldeak) {
}
