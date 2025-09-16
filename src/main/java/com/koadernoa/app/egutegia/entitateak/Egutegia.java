package com.koadernoa.app.egutegia.entitateak;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.FetchType;

@Getter
@Setter
@Entity
public class Egutegia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional=false)
    private Maila maila; // LEHENENGOA / BIGARRENA

    private LocalDate hasieraData;
    private LocalDate bukaeraData;

    private LocalDate lehenEbalBukaera;
    private LocalDate bigarrenEbalBukaera;
    // 3. ebaluazioa: bigarrenetik bukaerara

    @ManyToOne
    private Ikasturtea ikasturtea;

    @OneToMany(mappedBy = "egutegia", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<EgunBerezi> egunBereziak;
}
