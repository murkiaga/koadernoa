package com.koadernoa.app.objektuak.modulua.entitateak;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mintegi_modulu_baimena", uniqueConstraints = {
        @UniqueConstraint(name = "uk_mintegi_modulu_baimena_familia_eei", columnNames = {"familia_id", "eei_kodea"})
})
public class MintegiModuluBaimena {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id", nullable = false)
    private Familia familia;

    @Column(name = "eei_kodea", nullable = false, length = 50)
    private String eeiKodea;

    @Column(nullable = false)
    private boolean aktibo = true;

    @Column(length = 500)
    private String oharra;
}
