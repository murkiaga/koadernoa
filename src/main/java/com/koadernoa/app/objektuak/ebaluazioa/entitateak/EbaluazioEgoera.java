package com.koadernoa.app.objektuak.ebaluazioa.entitateak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_ebaluazio_egoera_kodea",
        columnNames = {"kodea"}
    )
)
public class EbaluazioEgoera {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kodea; // "EZ_AURKEZTUA", "EZ_EBAL_FALTA"...

    @Column(nullable = false)
    private String izena; // "Ez aurkeztua", "Ez ebaluatua faltengatik"...

    // Etorkizunerako beste flag batzuk jar zenezake (agerian/notetan kontatu, etab.)
}
