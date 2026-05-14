package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import java.util.List;

public record SeedZikloaEgoeraDto(
        String key,
        String familiaIzena,
        String familiaSlug,
        String izena,
        String maila,
        boolean existitzenDa,
        boolean partziala,
        boolean guztizSortuta,
        String egoeraTestua,
        int taldeKopurua,
        int sortutakoTaldeKopurua,
        List<SeedTaldeaEgoeraDto> taldeak) {
}
