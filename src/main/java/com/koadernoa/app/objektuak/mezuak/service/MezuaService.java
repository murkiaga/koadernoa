package com.koadernoa.app.objektuak.mezuak.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoPlangintzaKontrola;
import com.koadernoa.app.objektuak.mezuak.entitateak.Mezua;
import com.koadernoa.app.objektuak.mezuak.repository.MezuaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MezuaService {

    private final MezuaRepository mezuaRepository;
    private final IrakasleaRepository irakasleaRepository;

    @Transactional
    public Mezua bidaliJokabideDesegokiarenAbisua(
            JokabideDesegokia jokabidea, Irakaslea hartzailea) {
        if (jokabidea == null || jokabidea.getIrakaslea() == null || hartzailea == null) {
            throw new IllegalArgumentException("Jokabidea, bidaltzailea eta hartzailea beharrezkoak dira.");
        }
        Mezua mezua = new Mezua();
        mezua.setBidaltzailea(jokabidea.getIrakaslea());
        mezua.setHartzailea(hartzailea);
        mezua.setEdukia(sortuJokabideDesegokiarenEdukia(jokabidea));
        mezua.setBidalketaData(LocalDateTime.now());
        return mezuaRepository.save(mezua);
    }

    private String sortuJokabideDesegokiarenEdukia(JokabideDesegokia jokabidea) {
        String ikaslea = jokabidea.getIkaslea() == null
                ? "-" : String.join(" ", java.util.stream.Stream.of(
                                jokabidea.getIkaslea().getIzena(),
                                jokabidea.getIkaslea().getAbizena1(),
                                jokabidea.getIkaslea().getAbizena2())
                        .filter(zatia -> zatia != null && !zatia.isBlank())
                        .map(String::trim)
                        .toList());
        ikaslea = balioaEdoMarratxoa(ikaslea);
        String modulua = jokabidea.getModuloa() == null
                ? "-" : balioaEdoMarratxoa(jokabidea.getModuloa().getIzena());
        String taldea = jokabidea.getModuloa() == null || jokabidea.getModuloa().getTaldea() == null
                ? "-" : balioaEdoMarratxoa(jokabidea.getModuloa().getTaldea().getIzena());
        String data = jokabidea.getData() == null ? "-" : jokabidea.getData().toString();
        String sortzailea = balioaEdoMarratxoa(jokabidea.getIrakaslea().getIzena());
        return "Jokabide desegoki berria\n\n"
                + "Jokabide desegoki berri bat sortu da.\n\n"
                + "Ikaslea: " + ikaslea + "\n"
                + "Taldea: " + taldea + "\n"
                + "Modulua: " + modulua + "\n"
                + "Data: " + data + "\n"
                + "Sortzailea: " + sortzailea + "\n\n"
                + "Jokabide desegokien atalean kontsulta dezakezu.";
    }

    private String balioaEdoMarratxoa(String balioa) {
        return balioa == null || balioa.isBlank() ? "-" : balioa.trim();
    }

    @Transactional
    public int bidaliTaldeAldaketa(List<Koadernoa> koadernoak, String erabiltzailea, String edukia) {
        Set<Long> bidalitakoak = new LinkedHashSet<>();
        Irakaslea bidaltzailea = null;
        for (Koadernoa koadernoa : koadernoak) {
            if (koadernoa.getIrakasleak() == null) continue;
            for (Irakaslea hartzailea : koadernoa.getIrakasleak()) {
                if (hartzailea == null || hartzailea.getId() == null || !bidalitakoak.add(hartzailea.getId())) continue;
                if (bidaltzailea == null) {
                    bidaltzailea = irakasleaRepository.findByIzenaIgnoreCase("sistema")
                            .or(() -> irakasleaRepository.findByEmailaIgnoreCase("sistema@koadernoa.local"))
                            .or(() -> erabiltzailea == null ? Optional.empty()
                                    : irakasleaRepository.findByEmailaIgnoreCase(erabiltzailea)
                                            .or(() -> irakasleaRepository.findByIzenaIgnoreCase(erabiltzailea)))
                            .orElse(hartzailea);
                }
                Mezua mezua = new Mezua();
                mezua.setBidaltzailea(bidaltzailea);
                mezua.setHartzailea(hartzailea);
                mezua.setEdukia(edukia);
                mezua.setBidalketaData(LocalDateTime.now());
                mezuaRepository.save(mezua);
            }
        }
        return bidalitakoak.size();
    }


    @Transactional
    public int bidaliKoadernokoIrakasleei(Irakaslea bidaltzailea, Koadernoa koadernoa, String edukia) {
        if (bidaltzailea == null || koadernoa == null || koadernoa.getIrakasleak() == null) return 0;
        int bidaliak = 0;
        Set<Long> bidalitakoak = new LinkedHashSet<>();
        for (Irakaslea hartzailea : koadernoa.getIrakasleak()) {
            if (hartzailea == null || hartzailea.getId() == null || !bidalitakoak.add(hartzailea.getId())) continue;
            Mezua mezua = new Mezua();
            mezua.setBidaltzailea(bidaltzailea);
            mezua.setHartzailea(hartzailea);
            mezua.setEdukia(edukia);
            mezua.setBidalketaData(LocalDateTime.now());
            mezuaRepository.save(mezua);
            bidaliak++;
        }
        return bidaliak;
    }

    @Transactional
    public int bidaliPlangintzaAbisuak(
            Irakaslea bidaltzailea, List<KoadernoPlangintzaKontrola> kontrolak) {
        if (bidaltzailea == null || kontrolak == null || kontrolak.isEmpty()) return 0;

        Map<Long, IrakaslearenPlangintzaAbisua> abisuak = new LinkedHashMap<>();
        kontrolak.stream()
                .filter(KoadernoPlangintzaKontrola::abisuaBeharDu)
                .forEach(kontrola -> {
                    Koadernoa koadernoa = kontrola.koadernoa();
                    if (koadernoa == null || koadernoa.getIrakasleak() == null) return;
                    for (Irakaslea hartzailea : koadernoa.getIrakasleak()) {
                        if (hartzailea == null || hartzailea.getId() == null) continue;
                        abisuak.computeIfAbsent(hartzailea.getId(),
                                        id -> new IrakaslearenPlangintzaAbisua(hartzailea, new LinkedHashMap<>()))
                                .kontrolak()
                                .putIfAbsent(koadernoa.getId(), kontrola);
                    }
                });

        for (IrakaslearenPlangintzaAbisua abisua : abisuak.values()) {
            Mezua mezua = new Mezua();
            mezua.setBidaltzailea(bidaltzailea);
            mezua.setHartzailea(abisua.irakaslea());
            mezua.setEdukia(sortuPlangintzaAbisuarenEdukia(
                    abisua.irakaslea(), List.copyOf(abisua.kontrolak().values())));
            mezua.setBidalketaData(LocalDateTime.now());
            mezuaRepository.save(mezua);
        }
        return abisuak.size();
    }

    @Transactional
    public int bidaliPlangintzaAbisuak(List<KoadernoPlangintzaKontrola> kontrolak) {
        Irakaslea lehenHartzailea = kontrolak == null ? null : kontrolak.stream()
                .filter(KoadernoPlangintzaKontrola::abisuaBeharDu)
                .map(KoadernoPlangintzaKontrola::koadernoa)
                .filter(java.util.Objects::nonNull)
                .map(Koadernoa::getIrakasleak)
                .filter(java.util.Objects::nonNull)
                .flatMap(List::stream)
                .filter(irakaslea -> irakaslea != null && irakaslea.getId() != null)
                .findFirst()
                .orElse(null);
        if (lehenHartzailea == null) return 0;

        Irakaslea bidaltzailea = irakasleaRepository.findByIzenaIgnoreCase("sistema")
                .or(() -> irakasleaRepository.findByEmailaIgnoreCase("sistema@koadernoa.local"))
                .orElse(lehenHartzailea);
        return bidaliPlangintzaAbisuak(bidaltzailea, kontrolak);
    }

    private String sortuPlangintzaAbisuarenEdukia(
            Irakaslea hartzailea, List<KoadernoPlangintzaKontrola> kontrolak) {
        String izena = hartzailea.getIzena() == null || hartzailea.getIzena().isBlank()
                ? "irakasle"
                : hartzailea.getIzena().trim();
        StringBuilder edukia = new StringBuilder()
                .append("Kaixo ").append(izena).append(",\n\n")
                .append("Gogorarazten dizugu koadernoetan hurrengo 15 klase-egunetako ")
                .append("plangintza eginda egon behar dela.\n\n")
                .append("Une honetan honako koaderno hauetan plangintza osatu gabe dago:\n\n");

        for (KoadernoPlangintzaKontrola kontrola : kontrolak) {
            Koadernoa koadernoa = kontrola.koadernoa();
            String modulua = koadernoa.getModuloa() == null || koadernoa.getModuloa().getIzena() == null
                    ? "Modulurik gabe"
                    : koadernoa.getModuloa().getIzena();
            String taldea = koadernoa.getModuloa() == null || koadernoa.getModuloa().getTaldea() == null
                    || koadernoa.getModuloa().getTaldea().getIzena() == null
                    ? "Talderik gabe"
                    : koadernoa.getModuloa().getTaldea().getIzena();
            edukia.append('-').append(modulua).append(" – ").append(taldea).append('\n');
        }
        return edukia.append("\nMesedez, eguneratu denboralizazioa.").toString();
    }

    private record IrakaslearenPlangintzaAbisua(
            Irakaslea irakaslea, Map<Long, KoadernoPlangintzaKontrola> kontrolak) {}

    @Transactional
    public int bidaliEstatistikaFiltrotik(Irakaslea bidaltzailea, List<EstatistikaEbaluazioan> estatistikak, String edukia) {
        Set<Long> hartzaileIds = new LinkedHashSet<>();
        for (EstatistikaEbaluazioan e : estatistikak) {
            if (e.getKoadernoa() == null || e.getKoadernoa().getIrakasleak() == null) continue;
            e.getKoadernoa().getIrakasleak().forEach(ir -> {
                if (ir != null && ir.getId() != null) {
                    hartzaileIds.add(ir.getId());
                }
            });
        }

        int bidaliak = 0;
        for (Long hartzaileId : hartzaileIds) {
            if (bidaltzailea.getId().equals(hartzaileId)) continue;
            Irakaslea hartzailea = new Irakaslea();
            hartzailea.setId(hartzaileId);

            Mezua mezua = new Mezua();
            mezua.setBidaltzailea(bidaltzailea);
            mezua.setHartzailea(hartzailea);
            mezua.setEdukia(edukia);
            mezua.setBidalketaData(LocalDateTime.now());
            mezuaRepository.save(mezua);
            bidaliak++;
        }
        return bidaliak;
    }

    @Transactional(readOnly = true)
    public List<Mezua> jasotakoak(Long irakasleId) {
        return mezuaRepository.findByHartzaileaIdOrderByBidalketaDataDesc(irakasleId);
    }

    @Transactional(readOnly = true)
    public List<Mezua> bidaliEtaJasotakoak(Long irakasleId) {
        return mezuaRepository.findByBidaltzaileaIdOrHartzaileaIdOrderByBidalketaDataDesc(irakasleId, irakasleId);
    }

    @Transactional(readOnly = true)
    public List<Mezua> bidaliEtaJasotakoak(Long irakasleId, LocalDate dataHasiera, LocalDate dataAmaiera) {
        LocalDateTime hasiera = dataHasiera == null ? null : dataHasiera.atStartOfDay();
        LocalDateTime amaiera = dataAmaiera == null ? null : dataAmaiera.plusDays(1).atStartOfDay();
        return mezuaRepository.findBidaliEtaJasotakoakDatenArtean(irakasleId, hasiera, amaiera);
    }

    @Transactional
    public int ezabatuHautatuak(Long irakasleId, Collection<Long> mezuIds) {
        if (mezuIds == null || mezuIds.isEmpty()) return 0;
        return mezuaRepository.deleteHautatuak(irakasleId, mezuIds.stream().distinct().toList());
    }

    @Transactional(readOnly = true)
    public long irakurriGabekoKopurua(Long irakasleId) {
        return mezuaRepository.countByHartzaileaIdAndIrakurritaFalse(irakasleId);
    }

    @Transactional
    public Mezua markatuIrakurrita(Long mezuaId, Long hartzaileId) {
        Mezua mezua = mezuaRepository.findById(mezuaId)
                .orElseThrow(() -> new IllegalArgumentException("Mezua ez da aurkitu"));
        if (mezua.getHartzailea() == null || !hartzaileId.equals(mezua.getHartzailea().getId())) {
            throw new IllegalArgumentException("Mezu hau ezin duzu irakurri");
        }
        if (!mezua.isIrakurrita()) {
            mezua.setIrakurrita(true);
            mezua.setIrakurketaData(LocalDateTime.now());
            mezuaRepository.save(mezua);
        }
        return mezua;
    }
}
