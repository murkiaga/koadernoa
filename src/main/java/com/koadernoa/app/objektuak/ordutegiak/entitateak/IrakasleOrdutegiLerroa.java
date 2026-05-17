package com.koadernoa.app.objektuak.ordutegiak.entitateak;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class IrakasleOrdutegiLerroa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IrakasleOrdutegia irakasleOrdutegia;

    @Enumerated(EnumType.STRING)
    private Astegunak asteguna;
    private Integer orduZenbakia;
    private Integer saioKopurua;
    private String moduluKodea;
    private String moduluIzena;

    private String taldeKodea;

    @ManyToOne(fetch = FetchType.LAZY)
    private Taldea taldea;

    private String gelaKodea;
    private String gelaIzena;
    private String curso;
    private String grupo;
    private String nivel;
    private String turno;
    private String marco;
    private String tarea;
}
