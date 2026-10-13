package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
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

    @Column(nullable = false)
    private boolean kalifikagarria = false;

    @Column(precision = 5, scale = 2)
    private BigDecimal pisua;

    @Column(length = 2048)
    private String moodleEsteka;
    

    @ManyToOne
    private Koadernoa koadernoa;

    @ManyToOne
    private UnitateDidaktikoa unitatea;

    @OneToMany(mappedBy = "jarduera", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JardueraKalifikazioa> kalifikazioak = new ArrayList<>();
}
