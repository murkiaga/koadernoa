package com.koadernoa.app.objektuak.ordutegiak.entitateak;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_talde_ordutegia_hasiera",
        columnNames = {"taldea_id", "ikasturtea_id", "hasiera_data"}))
public class TaldeOrdutegia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) private Taldea taldea;
    @ManyToOne(optional = false) private Ikasturtea ikasturtea;
    @Column(nullable = false) private LocalDate hasieraData;
    @OneToMany(mappedBy = "taldeOrdutegia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaldeOrdutegiLerroa> lerroak = new ArrayList<>();
}
