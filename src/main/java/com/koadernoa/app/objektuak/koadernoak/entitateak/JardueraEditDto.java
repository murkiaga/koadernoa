package com.koadernoa.app.objektuak.koadernoak.entitateak;

import java.time.LocalDate;
import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JardueraEditDto {
	private Long id;
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate data;
    private String titulua;
    private String deskribapena;
    private boolean eginda;
    private float orduak;
    private String mota;
    private Long unitateaId;
    private String unitateaLabel;
    private boolean kalifikagarria;
    private BigDecimal pisua;
    private String moodleEsteka;

    public static JardueraEditDto from(Jarduera j){
        JardueraEditDto d = new JardueraEditDto();
        d.setId(j.getId());
        d.setData(j.getData());
        d.setTitulua(j.getTitulua());
        d.setDeskribapena(j.getDeskribapena());
        d.setOrduak(j.getOrduak());
        d.setMota(j.getMota());
        d.setKalifikagarria(j.isKalifikagarria());
        d.setPisua(j.getPisua());
        d.setMoodleEsteka(j.getMoodleEsteka());
        if (j.getUnitatea() != null) {
            d.setUnitateaId(j.getUnitatea().getId());
            d.setUnitateaLabel(j.getUnitatea().getKodea() + " — " + j.getUnitatea().getIzenburua());
        }
        return d;
    }
}
