package com.koadernoa.app.ethazi.entitateak.errubrikak;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_ebidentzia_maila", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_ebidentzia_maila", columnNames = {"ebidentzia_id", "maila_id"}))
@Getter @Setter
public class ErronkaEbidentziaMaila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ebidentzia_id") private ErronkaEbidentzia ebidentzia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "maila_id") private ErronkaErrubrikaMaila maila;
    @Column(nullable = false, columnDefinition = "TEXT") private String deskribapena = "";
}
