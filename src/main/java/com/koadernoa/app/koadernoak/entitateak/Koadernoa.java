package com.koadernoa.app.koadernoak.entitateak;

import java.util.List;

import com.koadernoa.app.egutegia.entitateak.Egutegia;
import com.koadernoa.app.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.modulua.entitateak.Moduloa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity

public class Koadernoa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Moduloa moduloa;

    @ManyToOne
    private Egutegia egutegia;

    @ManyToMany
    private List<Irakaslea> irakasleak;

    @OneToMany(mappedBy = "koadernoa")
    private List<Jarduera> jarduerak;

    @Embedded
    private Estatistikak estatistikak;

    @OneToMany(mappedBy = "koadernoa", cascade = CascadeType.ALL)
    private List<NotaFitxategia> notaFitxategiak;
    
    public String getIzena() {
        if (moduloa == null || egutegia == null || egutegia.getIkasturtea() == null)
            return "(koaderno osatu gabea)";
        return moduloa.getIzena() + " - " + egutegia.getIkasturtea().getIzena();
    }
}
