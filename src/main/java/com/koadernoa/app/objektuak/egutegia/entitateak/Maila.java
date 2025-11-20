package com.koadernoa.app.objektuak.egutegia.entitateak;

import java.util.List;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
@Entity
public class Maila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, unique=true)
    private String kodea;   // "LEHENENGOA", "BIGARRENA" ...
    private String izena;   // erakusteko izena (i18n gero)
    private Integer ordena; // opcional: zerrenda ordenatu
    private Boolean aktibo = true;
    
    @OneToMany(mappedBy = "maila", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EbaluazioMomentua> ebaluazioMomentuak;
}
