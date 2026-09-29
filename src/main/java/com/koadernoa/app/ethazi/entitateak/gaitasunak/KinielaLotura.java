package com.koadernoa.app.ethazi.entitateak.gaitasunak;

import java.util.LinkedHashSet;
import java.util.Set;
import com.koadernoa.app.ethazi.entitateak.Erronka;
import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="ethazi_kiniela_lotura", uniqueConstraints=@UniqueConstraint(name="uk_kiniela_modulua", columnNames={"adierazlea_id", "emaitza_id", "moduloa_id"}))
@Getter @Setter
public class KinielaLotura {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="adierazlea_id", nullable=false)
    private LorpenAdierazlea adierazlea;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="emaitza_id", nullable=false)
    private IkaskuntzaEmaitza emaitza;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="moduloa_id", nullable=false)
    private com.koadernoa.app.objektuak.modulua.entitateak.Moduloa moduloa;
    @Column(columnDefinition="TEXT")
    private String oharra = "";
    @ManyToMany
    @JoinTable(name="ethazi_kiniela_lotura_erronka",
        joinColumns=@JoinColumn(name="lotura_id"),
        inverseJoinColumns=@JoinColumn(name="erronka_id"),
        uniqueConstraints=@UniqueConstraint(name="uk_kiniela_lotura_erronka", columnNames={"lotura_id", "erronka_id"}))
    private Set<Erronka> erronkak = new LinkedHashSet<>();
}
