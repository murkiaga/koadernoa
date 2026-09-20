package com.koadernoa.app.objektuak.egutegia.entitateak;

import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

    @NotNull
    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @ColumnDefault("20")
    private Integer faltenMugaPortzentaia = 20;
    
    @OneToMany(mappedBy = "maila", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC, id ASC")
    private List<EbaluazioMomentua> ebaluazioMomentuak;
}
