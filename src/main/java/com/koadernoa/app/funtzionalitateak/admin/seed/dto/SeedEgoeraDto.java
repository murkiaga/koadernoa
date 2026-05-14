package com.koadernoa.app.funtzionalitateak.admin.seed.dto;

import java.util.List;

public record SeedEgoeraDto(
        Integer bertsioa,
        String iturria,
        String oharra,
        int zikloKopurua,
        int taldeKopurua,
        int moduluKopurua,
        List<SeedZikloaEgoeraDto> zikloak) {
}
