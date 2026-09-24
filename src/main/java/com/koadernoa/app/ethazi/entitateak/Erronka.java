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
@Entity @Table(name="ethazi_erronka") @Getter @Setter
public class Erronka {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, length=200) private String izena;
    @Column(nullable=false, columnDefinition="TEXT") private String deskribapena;
    @ManyToOne(optional=false) private Zikloa zikloa;
    @ManyToOne(optional=false) private Maila maila;
    @Column(nullable=false, length=20) private Hizkuntza hizkuntza;
    @Column(nullable=false) private LocalDate hasieraData;
    @Column(nullable=false) private LocalDate bukaeraData;
    @ManyToMany @JoinTable(name="ethazi_erronka_moduloa")
    private Set<Moduloa> moduluak = new LinkedHashSet<>();
}
