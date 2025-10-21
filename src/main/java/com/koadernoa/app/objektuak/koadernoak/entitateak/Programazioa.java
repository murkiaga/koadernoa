package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "programazioak")
public class Programazioa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "koaderno_id", unique = true, nullable = false)
    private Koadernoa koadernoa;

    @NotBlank
    private String izenburua;

    @Column(length = 2000)
    private String azalpena;

    @OneToMany(mappedBy = "programazioa", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC, id ASC")
    private List<Ebaluaketa> ebaluaketak = new ArrayList<>();

    public Programazioa() {}
    public Programazioa(Koadernoa koadernoa, String izenburua) {
        this.koadernoa = koadernoa;
        this.izenburua = izenburua;
    }

    // --- Kalkulu erosoak ---
    public int getOrduTotala() {
        if (ebaluaketak == null || ebaluaketak.isEmpty()) return 0;

        return ebaluaketak.stream()
            .filter(Objects::nonNull)
            .flatMap(eb -> eb.getUnitateak() == null ? java.util.stream.Stream.empty()
                                                     : eb.getUnitateak().stream())
            .mapToInt(u -> {
                // getOrduakEfektiboak() -> Integer bada:
                Integer h = u.getOrduakEfektiboak();
                return h != null ? h : 0;
                // getOrduakEfektiboak() primitibo int bada, nahikoa litzateke: return u.getOrduakEfektiboak();
            })
            .sum();
    }

    // --- Helper-ak (berriak) ---
    public void gehituEbaluaketa(Ebaluaketa e) {
        e.setProgramazioa(this);
        this.ebaluaketak.add(e);
    }
    public void kenduEbaluaketa(Ebaluaketa e) {
        e.setProgramazioa(null);
        this.ebaluaketak.remove(e);
    }

}
