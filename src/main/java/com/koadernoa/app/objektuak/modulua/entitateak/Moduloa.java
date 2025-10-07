package com.koadernoa.app.objektuak.modulua.entitateak;

import java.util.List;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

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
    
    @ManyToOne(optional=false)
    private Maila maila;

    @ManyToOne
    private Taldea taldea;

    @OneToMany(mappedBy = "moduloa")
    private List<Koadernoa> koadernoak;
}
