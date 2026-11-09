package com.koadernoa.app.ethazi.entitateak.kronograma;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.koadernoa.app.ethazi.entitateak.Erronka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/** Erronka bateko modulu guztiek partekatzen duten jarduera-kategoria. */
@Entity
@Table(name = "ethazi_erronka_jarduera_multzoa", indexes = {
    @Index(name = "ix_jarduera_multzoa_erronka_ordena", columnList = "erronka_id,ordena")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_jarduera_multzoa_erronka_izena", columnNames = {"erronka_id", "izena"}))
@Getter @Setter
public class ErronkaJardueraMultzoa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "erronka_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Erronka erronka;

    @Column(nullable = false, length = 200)
    private String izena;

    @Column(nullable = false)
    private Integer ordena = 0;
}
