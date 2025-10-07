package com.koadernoa.app.objektuak.modulua.entitateak;

import java.util.List;

import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
  indexes = {
    @Index(name="ix_ikaslea_hna", columnList="hna"),
    @Index(name="ix_ikaslea_nan", columnList="nan")
  },
  uniqueConstraints = {
    @UniqueConstraint(name="uk_ikaslea_hna", columnNames="hna") //hna bakarra
  }
)
public class Ikaslea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;
    private String abizena1;
    private String abizena2;
    private String nan; // DNI 
    private String hna; // DIE (unique)
    
    private String argazkiPath;

    @ManyToOne
    private Taldea taldea;

    @OneToMany(mappedBy = "ikaslea")
    private List<Matrikula> matrikulak;
}
