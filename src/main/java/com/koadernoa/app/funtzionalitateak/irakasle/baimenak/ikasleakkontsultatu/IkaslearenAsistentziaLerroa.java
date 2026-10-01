package com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import com.koadernoa.app.objektuak.jokabidea.service.IkasleEgunJardueraService.JokabideLaburpena;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class IkaslearenAsistentziaLerroa {
    private final Matrikula matrikula;
    private final int programaOrduak;
    private final int faltaOrduak;
    private final double faltaPortzentaia;
    private final Map<LocalDate, String> egunekoBalioak;
    private final Set<LocalDate> klaseEgunak;
    private final Map<LocalDate, String> oharrak;
    private final Map<LocalDate, java.util.List<JokabideLaburpena>> jokabideak;

    public String balioa(LocalDate eguna) {
        if (!klaseEgunak.contains(eguna)) {
            return "--";
        }
        return egunekoBalioak.getOrDefault(eguna, "");
    }

    public boolean baduOharra(LocalDate eguna) {
        return oharrak.containsKey(eguna);
    }

    public String oharra(LocalDate eguna) {
        return oharrak.getOrDefault(eguna, "");
    }

    public boolean baduJokabidea(LocalDate eguna) {
        return jokabideak.containsKey(eguna) && !jokabideak.get(eguna).isEmpty();
    }

    public java.util.List<JokabideLaburpena> jokabideak(LocalDate eguna) {
        return jokabideak.getOrDefault(eguna, java.util.List.of());
    }
}
