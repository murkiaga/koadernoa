package com.koadernoa.app.modulua.entitateak;

import com.koadernoa.app.zikloak.entitateak.Taldea;

import java.util.List;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import com.koadernoa.app.egutegia.entitateak.Maila;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Moduloa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;
    private String kodea; //"ADAT"
    
    @Enumerated(EnumType.STRING)
    private Maila maila;

    @ManyToOne
    private Taldea taldea;

    @OneToMany(mappedBy = "moduloa")
    private List<Koadernoa> koadernoak;
}
