package com.koadernoa.app.objektuak.koadernoak.entitateak;

import org.hibernate.annotations.OnDeleteAction;

import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ForeignKey;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(
  uniqueConstraints = @UniqueConstraint(
    name = "uk_asistentzia_saioa_matrikula",
    columnNames = {"saioa_id","matrikula_id"}
  ),
  indexes = {
    @Index(name="ix_asistentzia_saioa", columnList="saioa_id"),
    @Index(name="ix_asistentzia_matrikula", columnList="matrikula_id"),
    @Index(name="ix_asistentzia_egoera", columnList="egoera")
  }
)
public class Asistentzia {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional=false)
  private Saioa saioa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "matrikula_id",
              foreignKey = @ForeignKey(name = "fk_asistentzia_matrikula"))
  @org.hibernate.annotations.OnDelete(action = OnDeleteAction.CASCADE)
  private Matrikula matrikula;

  @Enumerated(EnumType.STRING)
  private AsistentziaEgoera egoera = AsistentziaEgoera.ETORRI;

  private String justifikazioTestu; // JUSTIFIKATUA kasuan, aukerakoa
  
  public enum AsistentziaEgoera { ETORRI, HUTS, JUSTIFIKATUA, BERANDU }
}