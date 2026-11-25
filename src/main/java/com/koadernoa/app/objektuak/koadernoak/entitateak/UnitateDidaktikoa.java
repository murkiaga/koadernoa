package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.util.ArrayList;
import java.util.List;
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
@Table(name = "unitate_didaktikoak",
       uniqueConstraints = {
           @UniqueConstraint(name="uk_programazioa_kodea", columnNames = {"programazio_id", "kodea"})
       })
public class UnitateDidaktikoa {

    public static final int KODEA_MAX_LENGTH = 20;
    public static final int IZENBURUA_MAX_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "programazio_id", nullable = false)
    private Programazioa programazioa;

    @NotBlank
    @Size(max = KODEA_MAX_LENGTH)
    @Column(nullable = false, length = KODEA_MAX_LENGTH)
    private String kodea; // "UD0", "UD1", ...

    @NotBlank
    @Size(max = IZENBURUA_MAX_LENGTH)
    @Column(nullable = false, length = IZENBURUA_MAX_LENGTH)
    private String izenburua;

    @Min(0)
    @Column(nullable = false)
    private int orduak = 0; // Azpijarduerarik ez badago, hau erabiliko da

    @Column(nullable = false)
    private int posizioa = 0; // ordena
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebaluaketa_id")
    private Ebaluaketa ebaluaketa;

    @OneToMany(mappedBy = "unitatea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posizioa ASC, id ASC")
    private List<JardueraPlanifikatua> azpiJarduerak = new ArrayList<>();

    public UnitateDidaktikoa() {}

    public UnitateDidaktikoa(Programazioa programazioa, String kodea, String izenburua, int orduak, int posizioa) {
        this.programazioa = programazioa;
        this.kodea = kodea;
        this.izenburua = izenburua;
        this.orduak = orduak;
        this.posizioa = posizioa;
    }

    /** Azpijarduerak badaude → horien batura; bestela → orduak. */
    public int getOrduakEfektiboak() {
        if (azpiJarduerak != null && !azpiJarduerak.isEmpty()) {
            return azpiJarduerak.stream().mapToInt(JardueraPlanifikatua::getOrduak).sum();
        }
        return orduak;
    }

    // helper-ak
    public void gehituAzpiJarduera(JardueraPlanifikatua aj) {
        aj.setUnitatea(this);
        this.azpiJarduerak.add(aj);
    }
    public void kenduAzpiJarduera(JardueraPlanifikatua aj) {
        aj.setUnitatea(null);
        this.azpiJarduerak.remove(aj);
    }

}
