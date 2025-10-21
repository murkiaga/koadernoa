package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "ebaluaketak")
public class Ebaluaketa {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;      // "1. Ebaluaketa", ...
    private Integer ordena;    // hurrenkera

    private LocalDate hasieraData;
    private LocalDate bukaeraData;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "programazioa_id", nullable = false)
    private Programazioa programazioa;

    @OneToMany(mappedBy = "ebaluaketa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posizioa ASC, id ASC")
    private List<UnitateDidaktikoa> unitateak = new ArrayList<>();
}
