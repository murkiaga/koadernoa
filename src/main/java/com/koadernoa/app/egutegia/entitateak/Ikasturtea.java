package com.koadernoa.app.egutegia.entitateak;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Ikasturtea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena; //"2024-2025"
    private boolean aktiboa;
    
    private LocalDate hasieraData;
    private LocalDate bukaeraData;
    
    private LocalDate lehenEbalBukaera;   // 1. ebaluazioaren bukaera
    private LocalDate bigarrenEbalBukaera; // 2. ebaluazioaren bukaera
    // 3. ebaluazioa: bigarrenetik bukaerara
    
    @Enumerated(EnumType.STRING)
    private Maila maila; // LEHENENGOA, BIGARRENA

    @OneToMany(mappedBy = "ikasturtea", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<EgunBerezi> egunBereziak;
}
