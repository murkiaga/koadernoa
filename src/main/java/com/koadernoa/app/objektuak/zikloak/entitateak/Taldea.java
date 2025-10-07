package com.koadernoa.app.objektuak.zikloak.entitateak;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
    
    //Tutore bakarra, eta irakasle bakoitzak talde bakarra: UNIQUE FK
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "tutore_id", unique = true)
    private Irakaslea tutorea;
}
