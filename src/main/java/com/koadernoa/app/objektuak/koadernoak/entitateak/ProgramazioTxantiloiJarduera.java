package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.math.BigDecimal;

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

    @Column(nullable = false)
    private boolean kalifikagarria = false;

    @Column(precision = 5, scale = 2)
    private BigDecimal pisua;

    @Column(length = 500)
    private String oharrak;

    private Integer ebaluaketaOrdena;

    @Column(length = 20)
    private String udKodea;

    @Column(length = 255)
    private String udIzenburua;
}
