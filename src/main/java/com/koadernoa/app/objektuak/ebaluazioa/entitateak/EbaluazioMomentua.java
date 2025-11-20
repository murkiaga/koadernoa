package com.koadernoa.app.objektuak.ebaluazioa.entitateak;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_ebaluazio_momentua_maila_kodea",
    columnNames = {"maila_id", "kodea"}
  )
)
public class EbaluazioMomentua {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Maila maila;

    @Column(nullable = false)
    private String kodea;   // "1_EBAL", "2_EBAL", "1_FINAL", "2_FINAL"...

    @Column(nullable = false)
    private String izena;   // erakusteko izena: "1. ebaluazioa", "2. finala"...

    private Integer ordena; // pantailan ordenatzeko

    private Boolean aktibo = true;
}
