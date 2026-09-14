package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
	GT01
	Sistema informatikoak instalatu eta konfiguratzeko gaitasuna
	TEKNIKOA
	SMR
 */

@Entity
@Table(name = "ethazi_gaitasuna")
@Getter
@Setter
@NoArgsConstructor
public class Gaitasuna {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zikloa_id", nullable = false)
    private Zikloa zikloa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private GaitasunMota mota;

    @Column(length = 30)
    private String kodea;

    @Column(nullable = false, length = 200)
    private String izena;

    @Column(columnDefinition = "TEXT")
    private String deskribapena;

    @OneToMany(
        mappedBy = "gaitasuna",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<GaitasunMaila> mailak = new ArrayList<>();
}