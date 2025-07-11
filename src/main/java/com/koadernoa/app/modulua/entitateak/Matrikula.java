package com.koadernoa.app.modulua.entitateak;

import java.util.List;

import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.zikloak.entitateak.Taldea;

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
public class Matrikula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Ikaslea ikaslea;

    @ManyToOne
    private Moduloa moduloa;

    @ManyToOne
    private Ikasturtea ikasturtea;

    private boolean pendiente; //aurreko urtetik dator?
}
