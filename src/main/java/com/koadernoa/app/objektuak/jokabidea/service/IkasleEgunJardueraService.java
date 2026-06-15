package com.koadernoa.app.objektuak.jokabidea.service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.jokabidea.entitateak.*;
import com.koadernoa.app.objektuak.jokabidea.repository.*;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.service.KoadernoaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IkasleEgunJardueraService {
    private final IkasleEgunOharraRepository oharraRepository;
    private final JokabideDesegokiaRepository jokabideRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final IrakasleaService irakasleaService;
    private final KoadernoaService koadernoaService;

    @Transactional(readOnly = true)
    public Testuingurua egiaztatu(Koadernoa koadernoa, Long ikasleaId, Authentication auth) {
        if (koadernoa == null || koadernoa.getId() == null) throw new IllegalArgumentException("Ez dago koaderno aktiborik aukeratuta.");
        Irakaslea irakaslea = irakasleaService.getLogeatutaDagoenIrakaslea(auth);
        if (!koadernoaService.irakasleakBadaukaSarbidea(irakaslea, koadernoa.getId())) throw new SecurityException("Ez duzu koaderno honetarako sarbiderik.");
        Matrikula matrikula = matrikulaRepository.findByIkasleaIdAndKoadernoaId(ikasleaId, koadernoa.getId())
            .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez dago koaderno aktiboan matrikulatuta."));
        return new Testuingurua(matrikula, irakaslea);
    }

    @Transactional
    public IkasleEgunOharra gordeOharra(Koadernoa koadernoa, Long ikasleaId, LocalDate data, String testua, Authentication auth) {
        if (data == null) throw new IllegalArgumentException("Data beharrezkoa da.");
        if (testua == null || testua.trim().isEmpty()) throw new IllegalArgumentException("Oharraren testua beharrezkoa da.");
        Testuingurua t = egiaztatu(koadernoa, ikasleaId, auth);
        IkasleEgunOharra o = oharraRepository.findByIkasleaIdAndKoadernoaIdAndData(ikasleaId, koadernoa.getId(), data).orElseGet(IkasleEgunOharra::new);
        o.setIkaslea(t.matrikula().getIkaslea()); o.setKoadernoa(t.matrikula().getKoadernoa());
        o.setIrakaslea(t.irakaslea()); o.setData(data); o.setTestua(testua.trim());
        return oharraRepository.save(o);
    }

    @Transactional
    public void ezabatuOharra(Koadernoa koadernoa, Long ikasleaId, LocalDate data, Authentication auth) {
        egiaztatu(koadernoa, ikasleaId, auth);
        oharraRepository.findByIkasleaIdAndKoadernoaIdAndData(ikasleaId, koadernoa.getId(), data).ifPresent(oharraRepository::delete);
    }

    @Transactional(readOnly = true)
    public Optional<IkasleEgunOharra> lortuOharra(Koadernoa koadernoa, Long ikasleaId, LocalDate data, Authentication auth) {
        egiaztatu(koadernoa, ikasleaId, auth);
        return oharraRepository.findByIkasleaIdAndKoadernoaIdAndData(ikasleaId, koadernoa.getId(), data);
    }

    @Transactional(readOnly = true)
    public Set<String> oharGakoak(Long koadernoId, LocalDate hasiera, LocalDate amaiera) {
        return oharraRepository.findByKoadernoaIdAndDataBetween(koadernoId, hasiera, amaiera).stream()
            .map(o -> gakoa(o.getIkaslea().getId(), o.getData())).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<String> jokabideGakoak(Long koadernoId, LocalDate hasiera, LocalDate amaiera) {
        return jokabideRepository.findByKoadernoaIdAndDataBetween(koadernoId, hasiera, amaiera).stream()
            .map(j -> gakoa(j.getIkaslea().getId(), j.getData())).collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Map<Long, List<JokabideLaburpena>> egunekoJokabideak(Long koadernoId, LocalDate data) {
        return jokabideRepository.findByKoadernoaIdAndData(koadernoId, data).stream().collect(Collectors.groupingBy(
            j -> j.getIkaslea().getId(), LinkedHashMap::new,
            Collectors.mapping(j -> new JokabideLaburpena(j.getId(), j.getPdfFilename()), Collectors.toList())));
    }

    public static String gakoa(Long ikasleaId, LocalDate data) { return ikasleaId + "|" + data; }
    public record Testuingurua(Matrikula matrikula, Irakaslea irakaslea) {}
    public record JokabideLaburpena(Long id, String pdfFilename) {}
}
