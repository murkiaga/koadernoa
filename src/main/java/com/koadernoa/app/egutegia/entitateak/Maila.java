package com.koadernoa.app.egutegia.entitateak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
}
