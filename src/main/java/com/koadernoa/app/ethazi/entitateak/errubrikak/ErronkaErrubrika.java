package com.koadernoa.app.ethazi.entitateak.errubrikak;

import java.util.ArrayList;
import java.util.List;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ethazi_erronka_errubrika", uniqueConstraints =
    @UniqueConstraint(name = "uk_erronka_errubrika_modulua", columnNames = {"erronka_id", "moduloa_id"}))
@Getter @Setter
public class ErronkaErrubrika {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "erronka_id") private Erronka erronka;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "moduloa_id") private Moduloa moduloa;
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC") private List<ErronkaErrubrikaMaila> mailak = new ArrayList<>();
    @OneToMany(mappedBy = "errubrika", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC") private List<ErronkaEbidentzia> ebidentziak = new ArrayList<>();
}
