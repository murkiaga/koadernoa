package com.koadernoa.app.ethazi.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.koadernoa.app.ethazi.entitateak.kronograma.KronogramaJardueraMota;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class KronogramaJardueraForm {
    private Long id;
    private String izenburua;
    private String deskribapena;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate hasieraData;
    private BigDecimal iraupenOrduak;
    private KronogramaJardueraMota mota;
    private Long moduloaId;
    private Long aurreikusitakoModuloaId;
}
