package com.koadernoa.app.ethazi.entitateak;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.koadernoa.app.objektuak.modulua.entitateak.*;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
@Entity @Table(name="ethazi_erronka", uniqueConstraints=@UniqueConstraint(name="uk_erronka_bertsioa", columnNames={"bertsio_taldea", "hizkuntza"})) @Getter @Setter
public class Erronka {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="bertsio_taldea") private Long bertsioTaldea;
    @Column(nullable=false, length=200) private String izena;
    @Column(nullable=false, columnDefinition="TEXT") private String deskribapena;
    @ManyToOne(optional=false) private Zikloa zikloa;
    @ManyToOne(optional=false) private Maila maila;
    // nullable DB mailan, ddl-auto-k aurreko instalazioak hautsi ez ditzan;
    // zerbitzuak beti ezartzen du eta abio-migrazioak datu zaharrak osatzen ditu.
    @ManyToOne @JoinColumn(name="ikasturtea_id") private Ikasturtea ikasturtea;
    @Column(nullable=false, length=20) private Hizkuntza hizkuntza;
    @Column(nullable=false) private LocalDate hasieraData;
    @Column(nullable=false) private LocalDate bukaeraData;
    @ManyToMany @JoinTable(name="ethazi_erronka_moduloa")
    private Set<Moduloa> moduluak = new LinkedHashSet<>();
}
