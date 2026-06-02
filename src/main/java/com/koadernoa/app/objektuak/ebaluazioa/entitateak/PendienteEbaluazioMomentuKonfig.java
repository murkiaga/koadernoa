package com.koadernoa.app.objektuak.ebaluazioa.entitateak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_pendiente_ebaluazio_momentu_kodea",
        columnNames = {"kodea"}
    )
)
public class PendienteEbaluazioMomentuKonfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kodea;

    @ManyToMany
    @JoinTable(
        name = "pendiente_ebaluazio_momentu_egoera",
        joinColumns = @JoinColumn(name = "pendiente_ebaluazio_momentu_konfig_id"),
        inverseJoinColumns = @JoinColumn(name = "ebaluazio_egoera_id")
    )
    @OrderBy("kodea ASC")
    private java.util.Set<EbaluazioEgoera> egoeraOnartuak = new java.util.LinkedHashSet<>();
}
