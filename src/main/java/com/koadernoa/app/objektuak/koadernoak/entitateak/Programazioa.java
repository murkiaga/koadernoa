package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "programazioak")
public class Programazioa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "koaderno_id", unique = true, nullable = false)
    private Koadernoa koadernoa;

    @NotBlank
    private String izenburua;

    @Column(length = 2000)
    private String azalpena;

    @OneToMany(mappedBy = "programazioa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posizioa ASC, id ASC")
    private List<UnitateDidaktikoa> unitateak = new ArrayList<>();

    public Programazioa() {}

    public Programazioa(Koadernoa koadernoa, String izenburua) {
        this.koadernoa = koadernoa;
        this.izenburua = izenburua;
    }

    // --- Kalkulu erosoak ---
    public int getOrduTotala() {
        return unitateak.stream().mapToInt(UnitateDidaktikoa::getOrduakEfektiboak).sum();
    }

    // --- Helper-ak ---
    public void gehituUnitatea(UnitateDidaktikoa ud) {
        ud.setProgramazioa(this);
        this.unitateak.add(ud);
    }

    public void kenduUnitatea(UnitateDidaktikoa ud) {
        ud.setProgramazioa(null);
        this.unitateak.remove(ud);
    }

}
