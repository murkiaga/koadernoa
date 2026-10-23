package com.koadernoa.app.ethazi.entitateak.errubrikak;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_zeharkako_ebidentzia_maila", uniqueConstraints =
    @UniqueConstraint(name = "uk_zeharkako_ebidentzia_maila", columnNames = {"ebidentzia_id", "maila_id"}))
@Getter @Setter
public class ZeharkakoEbidentziaMaila {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "ebidentzia_id") private ZeharkakoEbidentzia ebidentzia;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "maila_id") private ZeharkakoErrubrikaMaila maila;
    @Column(nullable = false, columnDefinition = "TEXT") private String deskribapena = "";
}
