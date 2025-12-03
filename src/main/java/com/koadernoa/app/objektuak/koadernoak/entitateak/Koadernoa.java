package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.util.List;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.persistence.CascadeType;
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
    
    @OneToMany(mappedBy = "koadernoa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<KoadernoOrdutegiBlokea> ordutegiak;

    @OneToMany(mappedBy = "koadernoa")
    private List<Jarduera> jarduerak;

    @OneToMany(mappedBy = "koadernoa", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstatistikaEbaluazioan> estatistikak;

    @OneToMany(mappedBy = "koadernoa", cascade = CascadeType.ALL)
    private List<NotaFitxategia> notaFitxategiak;
    
    public String getIzena() {
        if (moduloa == null || egutegia == null || egutegia.getIkasturtea() == null)
            return "(koaderno osatu gabea)";
        return moduloa.getIzena() + " - " + egutegia.getIkasturtea().getIzena();
    }
    
    public String getKodea() {
        if (moduloa == null || egutegia == null || egutegia.getIkasturtea() == null)
            return "(koaderno osatu gabea)";
        return moduloa.getKodea() + " - " + egutegia.getIkasturtea().getIzena();
    }
}
