package com.koadernoa.app.objektuak.mezuak.entitateak;

import java.time.LocalDateTime;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Mezua {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bidaltzailea_id")
    private Irakaslea bidaltzailea;

    @ManyToOne(optional = false)
    @JoinColumn(name = "hartzailea_id")
    private Irakaslea hartzailea;

    @Column(nullable = false, length = 2000)
    private String edukia;

    @Column(nullable = false)
    private boolean irakurrita = false;

    private LocalDateTime bidalketaData;
    private LocalDateTime irakurketaData;
}
