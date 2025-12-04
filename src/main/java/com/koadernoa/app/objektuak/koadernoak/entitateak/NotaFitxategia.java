package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.util.List;

import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class NotaFitxategia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //@Enumerated(EnumType.STRING)
    //private EbaluazioMota ebaluazioMota;

    private String fitxategiaUrl;

    @ManyToOne
    private Koadernoa koadernoa;
}
