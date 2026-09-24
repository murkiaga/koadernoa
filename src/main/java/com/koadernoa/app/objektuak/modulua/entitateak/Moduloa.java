package com.koadernoa.app.objektuak.modulua.entitateak;

import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Moduloa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;
    
    @Column(nullable = false, columnDefinition = "varchar(20) default 'ZEHAZTU_GABE'")
    private Hizkuntza hizkuntza = Hizkuntza.ZEHAZTU_GABE;
    
    private String kodea; //"RELO"
    @Column(name = "eei_kodea")
    private String eeiKodea; //"0225" kode honek RELO eta SALO lotu

    private Integer orduak;
    @Column(name = "dual_orduak")
    private Integer dualOrduak;
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean hautazkoa = false;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean aktibo = true;
    
    @ManyToOne(optional=false)
    private Maila maila;

    @ManyToOne
    private Taldea taldea;

    @OneToMany(mappedBy = "moduloa")
    private List<Koadernoa> koadernoak;
    
    @OneToMany(
	    mappedBy = "moduloa",
	    cascade = CascadeType.ALL,
	    orphanRemoval = true
	)
	@OrderBy("ordena ASC")
	private List<IkaskuntzaEmaitza> ikaskuntzaEmaitzak = new ArrayList<>();
}
