package com.koadernoa.app.zikloak.entitateak;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Zikloa {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena;

    @Enumerated(EnumType.STRING)
    private ZikloMaila maila; //ErdiMaila, GoiMaila
    
    @Enumerated(EnumType.STRING)
    private Familia familia;

    @OneToMany(mappedBy = "zikloa")
    private List<Taldea> taldeak;
}
