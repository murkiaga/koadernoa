package com.koadernoa.app.objektuak.modulua.service;

import java.net.IDN;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.security.ActiveDirectoryBilaketaService;
import com.koadernoa.app.security.AdErabiltzailea;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IkasleEmailAdService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]{1,64}@[^@\\s.]+(?:\\.[^@\\s.]+)+$");

    private final ActiveDirectoryBilaketaService adService;
    private final IkasleaRepository ikasleaRepository;
    private final TaldeaRepository taldeaRepository;

    public Aurrebista aurreikusi(Long taldeaId) {
        Taldea taldea = taldeaRepository.findById(taldeaId)
                .orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu."));
        List<AdErabiltzailea> adIkasleak = adService.bilatuTaldekoIkasleak(taldea.getIzena());
        Set<String> adEmailBikoiztuak = adIkasleak.stream().filter(a -> baliozkoEmaila(a.emaila()))
                .collect(Collectors.groupingBy(a -> normalizatuEmaila(a.emaila()), Collectors.counting()))
                .entrySet().stream().filter(e -> e.getValue() > 1).map(Map.Entry::getKey).collect(Collectors.toSet());
        List<Lerroa> lerroak = ikasleaRepository.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(taldeaId)
                .stream().map(i -> parekatu(i, adIkasleak, adEmailBikoiztuak)).toList();
        return new Aurrebista(taldea, lerroak,
                zenbatu(lerroak, Egoera.ZIURRA), zenbatu(lerroak, Egoera.ZALANTZA), zenbatu(lerroak, Egoera.EZ_AURKITUA));
    }

    @Transactional
    public GordeEmaitza gorde(Long taldeaId, Map<Long, Hautapena> hautapenak) {
        Aurrebista aurrebista = aurreikusi(taldeaId);
        Map<Long, Lerroa> zerbitzarikoProposamenak = aurrebista.lerroak().stream()
                .collect(Collectors.toMap(l -> l.ikaslea().getId(), Function.identity()));
        int eguneratuak = 0;
        int gatazkak = 0;
        Set<String> eskaeraEmailak = new HashSet<>();
        for (var entry : hautapenak.entrySet()) {
            Ikaslea ikaslea = ikasleaRepository.findById(entry.getKey()).orElse(null);
            Hautapena h = entry.getValue();
            String emaila = h.emaila() == null ? "" : h.emaila().trim().toLowerCase(Locale.ROOT);
            Lerroa proposamena = zerbitzarikoProposamenak.get(entry.getKey());
            boolean proposatutakoEmaila = proposamena != null && proposamena.egoera() != Egoera.EZ_AURKITUA
                    && (emaila.equals(normalizatuEmaila(proposamena.proposatutakoEmaila()))
                        || proposamena.hautagaiak().stream().anyMatch(a -> emaila.equals(normalizatuEmaila(a.emaila()))));
            if (ikaslea == null || ikaslea.getTaldea() == null || !taldeaId.equals(ikaslea.getTaldea().getId())
                    || !proposatutakoEmaila || !eskaeraEmailak.add(emaila)
                    || ikasleaRepository.existsByEmailIgnoreCaseAndIdNot(emaila, ikaslea.getId())
                    || (StringUtils.hasText(ikaslea.getEmail()) && !ikaslea.getEmail().equalsIgnoreCase(emaila) && !h.ordezkatu())) {
                gatazkak++;
                continue;
            }
            ikaslea.setEmail(emaila);
            ikasleaRepository.save(ikaslea);
            eguneratuak++;
        }
        return new GordeEmaitza(eguneratuak, aurrebista.ezAurkitua(), gatazkak);
    }

    private Lerroa parekatu(Ikaslea ikaslea, List<AdErabiltzailea> adIkasleak, Set<String> adEmailBikoiztuak) {
        String izena = normalizatu(ikaslea.getIzena());
        String abizenak = normalizatu(batu(ikaslea.getAbizena1(), ikaslea.getAbizena2()));
        List<AdErabiltzailea> zehatzak = adIkasleak.stream()
                .filter(a -> izena.equals(normalizatu(a.izena())) && abizenak.equals(normalizatu(a.abizenak())))
                .toList();
        if (zehatzak.size() == 1 && baliozkoEmaila(zehatzak.get(0).emaila())
                && !adEmailBikoiztuak.contains(normalizatuEmaila(zehatzak.get(0).emaila()))
                && !ikasleaRepository.existsByEmailIgnoreCaseAndIdNot(zehatzak.get(0).emaila().trim(), ikaslea.getId())) {
            AdErabiltzailea a = zehatzak.get(0);
            return new Lerroa(ikaslea, Egoera.ZIURRA, a.emaila(), List.of(a), !StringUtils.hasText(ikaslea.getEmail()));
        }
        List<AdErabiltzailea> hautagaiak = adIkasleak.stream()
                .filter(a -> antzekoa(izena, abizenak, a)).limit(8).toList();
        boolean emailikGabekoZehatza = zehatzak.stream().anyMatch(a -> !baliozkoEmaila(a.emaila()));
        Egoera egoera = hautagaiak.isEmpty() || emailikGabekoZehatza ? Egoera.EZ_AURKITUA : Egoera.ZALANTZA;
        return new Lerroa(ikaslea, egoera, null, hautagaiak, false);
    }

    private boolean antzekoa(String izena, String abizenak, AdErabiltzailea a) {
        String ai = normalizatu(a.izena());
        String aa = normalizatu(a.abizenak());
        return baliozkoEmaila(a.emaila()) && (izena.equals(ai) || abizenak.equals(aa)
                || tokenKomuna(izena, ai) && tokenKomuna(abizenak, aa));
    }

    private boolean tokenKomuna(String a, String b) {
        if (a.isBlank() || b.isBlank()) return false;
        Set<String> tokens = new HashSet<>(java.util.Arrays.asList(a.split(" ")));
        return java.util.Arrays.stream(b.split(" ")).anyMatch(tokens::contains);
    }

    static String normalizatu(String balioa) {
        if (balioa == null) return "";
        return Normalizer.normalize(balioa, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private static String batu(String... zatiak) {
        List<String> balioak = new ArrayList<>();
        for (String z : zatiak) if (StringUtils.hasText(z)) balioak.add(z.trim());
        return String.join(" ", balioak);
    }

    private boolean baliozkoEmaila(String emaila) {
        if (!StringUtils.hasText(emaila) || emaila.length() > 255 || !EMAIL.matcher(emaila.trim()).matches()) return false;
        try { IDN.toASCII(emaila.substring(emaila.indexOf('@') + 1)); return true; }
        catch (IllegalArgumentException ex) { return false; }
    }

    private String normalizatuEmaila(String emaila) {
        return emaila == null ? "" : emaila.trim().toLowerCase(Locale.ROOT);
    }

    private long zenbatu(List<Lerroa> lerroak, Egoera egoera) {
        return lerroak.stream().filter(l -> l.egoera() == egoera).count();
    }

    public enum Egoera { ZIURRA, ZALANTZA, EZ_AURKITUA }
    public record Lerroa(Ikaslea ikaslea, Egoera egoera, String proposatutakoEmaila,
                         List<AdErabiltzailea> hautagaiak, boolean lehenetsita) {}
    public record Aurrebista(Taldea taldea, List<Lerroa> lerroak, long ziurrak, long zalantzak, long ezAurkitua) {}
    public record Hautapena(String emaila, boolean ordezkatu) {}
    public record GordeEmaitza(int eguneratuak, long parekatuGabe, int gatazkak) {}
}
