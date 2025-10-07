package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;
import java.util.List;

import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Jarduera {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulua;
    private String deskribapena;
    private LocalDate data;
    private float orduak;
    
    private String mota; //"planifikatua", "eginda", "ebaluazioa", etab.
    

    @ManyToOne
    private Koadernoa koadernoa;
}
