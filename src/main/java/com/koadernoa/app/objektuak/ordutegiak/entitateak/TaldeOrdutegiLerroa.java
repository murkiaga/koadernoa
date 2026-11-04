package com.koadernoa.app.objektuak.ordutegiak.entitateak;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_talde_ordutegi_gelaxka",
        columnNames = {"talde_ordutegia_id", "asteguna", "ordu_zenbakia"}))
public class TaldeOrdutegiLerroa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private TaldeOrdutegia taldeOrdutegia;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Astegunak asteguna;
    @Column(nullable = false) private Integer orduZenbakia;
    @ManyToOne(optional = false) private Moduloa moduloa;
}
