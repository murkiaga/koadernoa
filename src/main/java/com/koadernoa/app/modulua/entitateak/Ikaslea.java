package com.koadernoa.app.modulua.entitateak;

import java.util.List;

import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Ikaslea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;
    private String nan;

    @ManyToOne
    private Taldea taldea;

    @OneToMany(mappedBy = "ikaslea")
    private List<Matrikula> matrikulak;
}
