package com.koadernoa.app.objektuak.modulua.entitateak;


import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import lombok.Getter;
import lombok.Setter;

@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_matrikula_ikasle_koadernoa",
    columnNames = {"ikaslea_id", "koadernoa_id"}
  ), //Matrikulazio bikoiztuak saihesteko
  indexes = {
    @Index(name = "ix_matrikula_koadernoa", columnList = "koadernoa_id"),
    @Index(name = "ix_matrikula_egoera", columnList = "egoera"),
    @Index(name = "ix_matrikula_koadernoa_egoera", columnList = "koadernoa_id, egoera")
  }
) 
@Getter
@Setter
@Entity
public class Matrikula {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ikaslea_id")
    private Ikaslea ikaslea;

    @ManyToOne(optional=false)
    @JoinColumn(name = "koadernoa_id")
    private Koadernoa koadernoa;

    @Enumerated(EnumType.STRING)
    private MatrikulaEgoera egoera = MatrikulaEgoera.MATRIKULATUA;
    
    private String oharra; // arrazoia: “lanarekin uztartu ezina”, “zentrutik baja”...
}
