package com.koadernoa.app.zikloak.entitateak;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Taldea {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String izena; //"1SMA", "2SMD"

    @ManyToOne
    private Zikloa zikloa;
}
