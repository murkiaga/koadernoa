package com.koadernoa.app.koadernoak.entitateak;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "jarduera_planifikatua")
public class JardueraPlanifikatua {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "unitate_id", nullable = false)
    private UnitateDidaktikoa unitatea;

    @NotBlank
    @Column(nullable = false)
    private String izenburua;

    @Min(0)
    @Column(nullable = false)
    private int orduak = 0;
    
    @Min(0)
    @Column(nullable=false)
    private int posizioa = 0;

    public JardueraPlanifikatua() {}

    public JardueraPlanifikatua(UnitateDidaktikoa unitatea, String izenburua, int orduak) {
        this.unitatea = unitatea;
        this.izenburua = izenburua;
        this.orduak = orduak;
    }
}

