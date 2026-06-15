package com.koadernoa.app.objektuak.jokabidea.entitateak;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "portaera_arrazoia", indexes = @Index(name = "ix_portaera_arrazoia_aktibo_ordena", columnList = "aktibo, ordena"))
public class PortaeraArrazoia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String kodea;
    @Column(nullable = false, length = 2000)
    private String testua;
    @Column(nullable = false)
    private boolean aktibo = true;
    @Column(nullable = false)
    private boolean defektuzkoa;
    @Column(nullable = false)
    private int ordena;
}
