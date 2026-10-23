package com.koadernoa.app.ethazi.entitateak;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.koadernoa.app.ethazi.entitateak.errubrikak.ErronkaEbidentziaTaldeNota;

@Entity
@Table(name = "ethazi_erronka_taldea", uniqueConstraints = {
    @UniqueConstraint(name = "uk_erronka_taldea_ordena", columnNames = {"erronka_id", "ordena"}),
    @UniqueConstraint(name = "uk_erronka_taldea_izena", columnNames = {"erronka_id", "izena"})
})
@Getter @Setter
public class ErronkaTaldea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "erronka_id") private Erronka erronka;
    @Column(nullable = false) private Integer ordena;
    @Column(nullable = false, length = 100) private String izena;
    @OneToMany(mappedBy = "taldea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC") private List<ErronkaTaldeKidea> kideak = new ArrayList<>();
    @OneToMany(mappedBy = "taldea", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErronkaEbidentziaTaldeNota> notak = new ArrayList<>();

    @Transient
    public String getKideenIzenak() {
        return kideak.stream().map(k -> k.getIkaslea().getIzenOsoa())
            .collect(java.util.stream.Collectors.joining("\n"));
    }
}
