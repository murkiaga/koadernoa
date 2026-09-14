package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;

@Entity
@Table(name = "ethazi_lorpen_adierazlea")
@Getter
@Setter
@NoArgsConstructor
public class LorpenAdierazlea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gaitasun_maila_id", nullable = false)
    private GaitasunMaila gaitasunMaila;

    @Column(nullable = false)
    private Integer ordena;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String deskribapena;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "ethazi_lorpen_adierazlea_ikaskuntza_emaitza",
        joinColumns = @JoinColumn(
            name = "lorpen_adierazlea_id",
            nullable = false,
            foreignKey = @ForeignKey(
                name = "fk_lorpen_adierazlea_ie_lorpen"
            )
        ),
        inverseJoinColumns = @JoinColumn(
            name = "ikaskuntza_emaitza_id",
            nullable = false,
            foreignKey = @ForeignKey(
                name = "fk_lorpen_adierazlea_ie_ie"
            )
        ),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_lorpen_adierazlea_ikaskuntza_emaitza",
            columnNames = {
                "lorpen_adierazlea_id",
                "ikaskuntza_emaitza_id"
            }
        )
    )
    private Set<IkaskuntzaEmaitza> ikaskuntzaEmaitzak = new LinkedHashSet<>();
}