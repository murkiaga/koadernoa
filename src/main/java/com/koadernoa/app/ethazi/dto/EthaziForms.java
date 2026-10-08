package com.koadernoa.app.ethazi.dto;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;

import org.springframework.format.annotation.DateTimeFormat;

import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

import lombok.Getter;
import lombok.Setter;

/** Web forms deliberately contain identifiers, never bindable JPA relationships. */
public final class EthaziForms {
    private EthaziForms() {}
    @Getter @Setter
    public static class ErronkaForm {
        private Long zikloaId;
        private Long mailaId;
        private String izena;
        private String deskribapena;
        private Hizkuntza hizkuntza = Hizkuntza.ZEHAZTU_GABE;
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        private java.time.LocalDate hasieraData;
        @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
        private java.time.LocalDate bukaeraData;
        private Set<Long> moduloIds = new LinkedHashSet<>();
    }

    @Getter @Setter
    public static class ErronkaErrubrikaForm {
        private List<ErronkaMailaForm> mailak = new ArrayList<>();
        private List<ErronkaEbidentziaForm> ebidentziak = new ArrayList<>();
    }

    @Getter @Setter
    public static class ErronkaMailaForm {
        private Long id;
        private String izena;
        private BigDecimal balioa;
    }

    @Getter @Setter
    public static class ErronkaEbidentziaForm {
        private Long id;
        private String deskribapena;
        private BigDecimal pisua;
        private Set<Long> adierazleaIds = new LinkedHashSet<>();
        private List<ErronkaEbidentziaMailaForm> mailak = new ArrayList<>();
        private List<ErronkaTaldeNotaForm> taldeNotak = new ArrayList<>();
    }

    @Getter @Setter
    public static class ErronkaEbidentziaMailaForm {
        private Long mailaId;
        private String deskribapena;
    }

    @Getter @Setter
    public static class ErronkaTaldeNotaForm {
        private Long taldeaId;
        private Long mailaId;
        private String mailaIzena;
        private String ondoEgindakoak;
        private String hobetuBeharrekoak;
    }

    @Getter @Setter
    public static class ErronkaTaldeEsleipenForm {
        private List<ErronkaIkasleTaldeForm> ikasleak = new ArrayList<>();
    }

    @Getter @Setter
    public static class ErronkaIkasleTaldeForm {
        private Long ikasleaId;
        private Long taldeaId;
    }


    @Getter @Setter
    public static class EreduaForm {
        private Long zikloaId;
        private GaitasunMota mota = GaitasunMota.TEKNIKOA;
        private String izena;
        private String izenaEs;
        private String izenaEn;
    }

    @Getter @Setter
    public static class MailaForm {
        private String izena;
        private String izenaEs;
        private String izenaEn;
    }

    @Getter @Setter
    public static class GaitasunaForm {
        private Long zikloaId;
        private GaitasunMota mota = GaitasunMota.TEKNIKOA;
        private String kodea;
        private String deskribapena;
        private String deskribapenaEs;
        private String deskribapenaEn;
        private List<GaitasunMailaForm> mailak = new ArrayList<>();
    }

    @Getter @Setter
    public static class GaitasunMailaForm {
        private Long mailaId;
        private String deskribapena;
        private String deskribapenaEs;
        private String deskribapenaEn;
    }

    @Getter @Setter
    public static class AdierazleaForm {
        private String deskribapena;
        private String deskribapenaEs;
        private String deskribapenaEn;
        private Set<Long> emaitzaIds = new LinkedHashSet<>();
    }

    @Getter @Setter
    public static class EmaitzaForm {
        private Long zikloaId;
        private Long moduloaId;
        private String kodea;
        private Integer ordena;
        private String deskribapena;
        private String deskribapenaEs;
        private String deskribapenaEn;
    }
}
