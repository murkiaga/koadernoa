package com.koadernoa.app.objektuak.egutegia.entitateak;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Ikasturtea {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena; // Adib. "2025-2026"
    private boolean aktiboa;

    @OneToMany(mappedBy = "ikasturtea", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Egutegia> egutegiak;
}
