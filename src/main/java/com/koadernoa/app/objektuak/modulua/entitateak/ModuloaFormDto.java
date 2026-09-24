package com.koadernoa.app.objektuak.modulua.entitateak;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModuloaFormDto {
    private Long id;
    @NotNull
    private Hizkuntza hizkuntza = Hizkuntza.ZEHAZTU_GABE;

    @NotBlank
    private String izena;

    @NotBlank
    private String kodea;
    
    @NotBlank
    private String eeiKodea;

    @NotNull
    private Integer orduak;
    
    @NotNull
    private Integer dualOrduak;

    private boolean hautazkoa;

    private boolean aktibo = true;

    @NotNull
    private Long mailaId;

    @NotNull
    private Long taldeaId;
}
