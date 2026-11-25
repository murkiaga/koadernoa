package com.koadernoa.app.objektuak.koadernoak.entitateak;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "jarduera_planifikatua")
public class JardueraPlanifikatua {

    public static final int IZENBURUA_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "unitate_id", nullable = false)
    private UnitateDidaktikoa unitatea;

    @NotBlank
    @Size(max = IZENBURUA_MAX_LENGTH)
    @Column(nullable = false, length = IZENBURUA_MAX_LENGTH)
    private String izenburua;

    @Min(0)
    @Column(nullable = false)
    private int orduak = 0;
    
    @Min(0)
    @Column(nullable=false)
    private int posizioa = 0;

    @Column(nullable = false)
    private boolean kalifikagarria = false;

    public JardueraPlanifikatua() {}

    public JardueraPlanifikatua(UnitateDidaktikoa unitatea, String izenburua, int orduak) {
        this.unitatea = unitatea;
        this.izenburua = izenburua;
        this.orduak = orduak;
    }
}
