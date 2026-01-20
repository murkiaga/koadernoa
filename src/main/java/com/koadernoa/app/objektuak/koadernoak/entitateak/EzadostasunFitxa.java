package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EzadostasunFitxa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "estatistika_id", nullable = false, unique = true)
    private EstatistikaEbaluazioan estatistika;

    private int emandakoBlokeKopurua;
    private int emandakoOrduKopurua;
    private Double ikasleenBertaratzePortzentaia;
    private Integer gaindituPortzentaia;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String zuzentzeJarduerak;
    private String zuzentzeJarduerakArduraduna;

    private LocalDate jarraipenData;
    private String jarraipenArduradunak;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String hartutakoErabakiak;

    private LocalDate itxieraData;
    private String itxieraArduraduna;
    private Boolean ezadostasunaZuzenduta;
}
