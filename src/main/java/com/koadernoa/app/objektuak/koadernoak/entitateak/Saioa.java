package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_saioa_koadernoa_data_slot",
    columnNames = {"koadernoa_id","data","hasieraSlot"}
  ),
  indexes = {
    @Index(name="ix_saioa_koadernoa_data", columnList="koadernoa_id, data"),
    @Index(name="ix_saioa_data", columnList="data")
  }
)
public class Saioa {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false)
  private Koadernoa koadernoa;

  private LocalDate data;

  private int hasieraSlot;   // 1..12 (zure eredua)
  private int iraupenaSlot;  // >=1

  @ManyToOne
  private KoadernoOrdutegiBlokea iturburuBlokea; // aukerakoa; saioa nondik dator

  @Enumerated(EnumType.STRING)
  private SaioEgoera egoera = SaioEgoera.AKTIBOA; 
  // AKTIBOA, EZEZTATUA (ez-lektiboa, jaieguna, ordezkapenagatik bertan behera), ORDEZKATUA...
  
  private String oharra; // aukerakoa
  
  public enum SaioEgoera { AKTIBOA, EZEZTATUA, ORDEZKATUA }
}


