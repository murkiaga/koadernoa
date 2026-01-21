package com.koadernoa.app.objektuak.koadernoak.entitateak;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "programazio_txantiloi_jarduerak")
public class ProgramazioTxantiloiJarduera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "txantiloi_id", nullable = false)
    private ProgramazioTxantiloi txantiloi;

    @Column(nullable = false)
    private String izenburua;

    @Column(length = 2000)
    private String deskribapena;

    private Integer iraupenaMin;

    private Integer ordena;

    @Column(length = 500)
    private String oharrak;
}
