package com.koadernoa.app.objektuak.egutegia.entitateak;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class EgunBerezi {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate data;
    private String deskribapena;

    @Enumerated(EnumType.STRING)
    private EgunMota mota; // LEKTIBOA, EZ_LEKTIBOA, JAIEGUNA, ORDEZKATUA

    @Enumerated(EnumType.STRING)
    private Astegunak ordezkatua; // ASTELEHENA-...-OSTIRALA

    @ManyToOne
    private Egutegia egutegia;
}

