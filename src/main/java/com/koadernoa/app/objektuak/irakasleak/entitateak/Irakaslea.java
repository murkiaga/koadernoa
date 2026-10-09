package com.koadernoa.app.objektuak.irakasleak.entitateak;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_irakaslea_emaila", columnNames = "emaila"))
public class Irakaslea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String izena;
    private String emaila;
    private String pasahitza;
    private String kontu_mota;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "familia_id")
    private Familia mintegia;
    
    @ManyToMany(mappedBy = "irakasleak")
    private List<Koadernoa> koadernoak;
    
    @Enumerated(EnumType.STRING)
    private Rola rola;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "irakaslea_baimenak",
        joinColumns = @JoinColumn(name = "irakaslea_id"),
        indexes = @Index(name = "ix_irakaslea_baimenak_irakaslea", columnList = "irakaslea_id"),
        uniqueConstraints = @UniqueConstraint(
            name = "uk_irakaslea_baimena",
            columnNames = {"irakaslea_id", "baimena"}
        )
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "baimena", nullable = false)
    private Set<Baimena> baimenak = new LinkedHashSet<>();
    
    @OneToOne(mappedBy = "tutorea", fetch = FetchType.LAZY)
    private Taldea tutoreTaldea;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordezkoa_id")
    private Irakaslea ordezkoa;

}
