package com.koadernoa.app.objektuak.modulua.entitateak;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ModuloaFormDto {
    private Long id;

    @NotBlank
    private String izena;

    @NotBlank
    private String kodea;
    
    @NotBlank
    private String eeiKodea;

    @NotNull
    private Long mailaId;

    @NotNull
    private Long taldeaId;
}