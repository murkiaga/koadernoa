package com.koadernoa.app.objektuak.logak.entitateak;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class LogSarrera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private LogMota mota;

    private LocalDateTime data;

    private Long eragileaId;
    private String eragileaIzena;
    private String eragileaEmaila;

    private String entitateMota;
    private Long entitateId;

    private String deskribapena;
}
