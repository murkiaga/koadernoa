package com.koadernoa.app.objektuak.ordutegiak.entitateak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
public class IrakasleOrdutegia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Ikasturtea ikasturtea;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Irakaslea irakaslea;

    private String xmlIrakasleKodea;
    private String xmlIrakasleIzena;
    private String jatorria;
    private LocalDateTime inportazioData;

    @OneToMany(mappedBy = "irakasleOrdutegia", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IrakasleOrdutegiLerroa> lerroak = new ArrayList<>();

    public void gehituLerroa(IrakasleOrdutegiLerroa lerroa) {
        lerroak.add(lerroa);
        lerroa.setIrakasleOrdutegia(this);
    }
}
