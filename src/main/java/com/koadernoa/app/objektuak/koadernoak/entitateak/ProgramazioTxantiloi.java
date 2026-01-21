package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "programazio_txantiloiak")
public class ProgramazioTxantiloi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String izena;

    @Column(nullable = false)
    private LocalDateTime sortzeData;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Irakaslea irakaslea;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Moduloa moduloa;

    @ManyToOne(fetch = FetchType.LAZY)
    private Koadernoa iturburuKoadernoa;

    @OneToMany(mappedBy = "txantiloi", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordena ASC, id ASC")
    private List<ProgramazioTxantiloiJarduera> jarduerak = new ArrayList<>();

    @Transient
    public int getJardueraKopurua() {
        return jarduerak == null ? 0 : jarduerak.size();
    }

    @Transient
    public int getGuztiraMin() {
        if (jarduerak == null) return 0;
        return jarduerak.stream()
            .mapToInt(j -> j.getIraupenaMin() == null ? 0 : j.getIraupenaMin())
            .sum();
    }
}
