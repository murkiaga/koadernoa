package com.koadernoa.app.funtzionalitateak.admin.seed.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedEgoeraDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedFamiliaDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedImportRequest;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedImportResult;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedKatalogoaDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedMailaDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedModuloaDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedTaldeaDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedTaldeaEgoeraDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedZikloaDto;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedZikloaEgoeraDto;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.MailaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.ZikloMaila;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;
import com.koadernoa.app.objektuak.zikloak.repository.FamiliaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;
import com.koadernoa.app.objektuak.zikloak.repository.ZikloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KatalogoAkademikoaSeedService {

    private static final String KATALOGOA_PATH = "seed/katalogo-akademikoa.json";

    private final ObjectMapper objectMapper;
    private final MailaRepository mailaRepository;
    private final FamiliaRepository familiaRepository;
    private final ZikloaRepository zikloaRepository;
    private final TaldeaRepository taldeaRepository;
    private final ModuloaRepository moduloaRepository;

    @Transactional(readOnly = true)
    public SeedEgoeraDto kalkulatuEgoera() {
        SeedKatalogoaDto katalogoa = irakurriKatalogoa();
        List<SeedZikloaEgoeraDto> zikloEgoerak = new ArrayList<>();
        int taldeKopurua = 0;
        int moduluKopurua = 0;

        for (int f = 0; f < safe(katalogoa.familiak()).size(); f++) {
            SeedFamiliaDto familiaDto = katalogoa.familiak().get(f);
            Optional<Familia> familia = bilatuFamilia(familiaDto.izena(), familiaDto.slug());
            for (int z = 0; z < safe(familiaDto.zikloak()).size(); z++) {
                SeedZikloaDto zikloaDto = familiaDto.zikloak().get(z);
                Optional<Zikloa> zikloa = familia.flatMap(fam -> zikloaRepository.findByIzenaIgnoreCaseAndFamilia(zikloaDto.izena(), fam));
                List<SeedTaldeaEgoeraDto> taldeEgoerak = new ArrayList<>();
                int sortutakoTaldeKopurua = 0;
                for (int t = 0; t < safe(zikloaDto.taldeak()).size(); t++) {
                    SeedTaldeaDto taldeaDto = zikloaDto.taldeak().get(t);
                    boolean taldeaBadago = zikloa.flatMap(zi -> taldeaRepository.findByIzenaIgnoreCaseAndZikloa(taldeaDto.izena(), zi)).isPresent();
                    if (taldeaBadago) {
                        sortutakoTaldeKopurua++;
                    }
                    taldeKopurua++;
                    moduluKopurua += safe(taldeaDto.moduluak()).size();
                    taldeEgoerak.add(new SeedTaldeaEgoeraDto(
                            taldeKey(f, z, t),
                            taldeaDto.izena(),
                            taldeaBadago,
                            taldeaBadago ? "Jada sortuta" : "Sortu gabe",
                            safe(taldeaDto.moduluak()).size()));
                }
                int taldeak = safe(zikloaDto.taldeak()).size();
                boolean zikloaBadago = zikloa.isPresent();
                boolean guztizSortuta = zikloaBadago && taldeak == sortutakoTaldeKopurua;
                boolean partziala = zikloaBadago && sortutakoTaldeKopurua > 0 && !guztizSortuta;
                String egoera = guztizSortuta ? "Jada sortuta" : (zikloaBadago && sortutakoTaldeKopurua > 0 ? "Partzialki sortuta" : "Sortu gabe");
                zikloEgoerak.add(new SeedZikloaEgoeraDto(
                        zikloKey(f, z),
                        familiaDto.izena(),
                        familiaDto.slug(),
                        zikloaDto.izena(),
                        zikloaDto.maila(),
                        zikloaBadago,
                        partziala,
                        guztizSortuta,
                        egoera,
                        taldeak,
                        sortutakoTaldeKopurua,
                        taldeEgoerak));
            }
        }

        return new SeedEgoeraDto(
                katalogoa.bertsioa(),
                katalogoa.iturria(),
                katalogoa.oharra(),
                zikloEgoerak.size(),
                taldeKopurua,
                moduluKopurua,
                zikloEgoerak);
    }

    @Transactional
    public SeedImportResult inportatu(SeedImportRequest request) {
        Set<String> hautatutakoZikloak = new HashSet<>(safe(request.getZikloak()));
        Set<String> hautatutakoTaldeak = new HashSet<>(safe(request.getTaldeak()));
        if (hautatutakoZikloak.isEmpty() && hautatutakoTaldeak.isEmpty()) {
            throw new IllegalArgumentException("Aukeratu gutxienez ziklo edo talde bat.");
        }

        SeedKatalogoaDto katalogoa = irakurriKatalogoa();
        Map<String, SeedMailaDto> mailak = new HashMap<>();
        for (SeedMailaDto maila : safe(katalogoa.mailak())) {
            mailak.put(norm(maila.kodea()), maila);
        }

        Kontagailuak k = new Kontagailuak();
        for (int f = 0; f < safe(katalogoa.familiak()).size(); f++) {
            SeedFamiliaDto familiaDto = katalogoa.familiak().get(f);
            for (int z = 0; z < safe(familiaDto.zikloak()).size(); z++) {
                SeedZikloaDto zikloaDto = familiaDto.zikloak().get(z);
                String zikloKey = zikloKey(f, z);
                List<Integer> taldeIndizeak = hautatutakoTaldeIndizeak(zikloKey, zikloaDto, hautatutakoZikloak, hautatutakoTaldeak);
                if (taldeIndizeak.isEmpty()) {
                    continue;
                }

                Familia familia = ensureFamilia(familiaDto, k);
                Zikloa zikloa = ensureZikloa(zikloaDto, familia, k);

                for (Integer t : taldeIndizeak) {
                    SeedTaldeaDto taldeaDto = zikloaDto.taldeak().get(t);
                    Optional<Taldea> existingTaldea = taldeaRepository.findByIzenaIgnoreCaseAndZikloa(taldeaDto.izena(), zikloa);
                    Taldea taldea;
                    if (existingTaldea.isPresent()) {
                        k.lehendikZeudenak++;
                        taldea = existingTaldea.get();
                    } else {
                        taldea = new Taldea();
                        taldea.setIzena(taldeaDto.izena());
                        taldea.setZikloa(zikloa);
                        taldea = taldeaRepository.save(taldea);
                        k.sortutakoTaldeak++;
                    }
                    ensureModuluak(taldeaDto, taldea, mailak, k);
                }
            }
        }

        return new SeedImportResult(k.sortutakoMailak, k.sortutakoFamiliak, k.sortutakoZikloak,
                k.sortutakoTaldeak, k.sortutakoModuluak, k.lehendikZeudenak);
    }

    private List<Integer> hautatutakoTaldeIndizeak(String zikloKey, SeedZikloaDto zikloaDto, Set<String> hautatutakoZikloak, Set<String> hautatutakoTaldeak) {
        List<Integer> emaitza = new ArrayList<>();
        for (int t = 0; t < safe(zikloaDto.taldeak()).size(); t++) {
            String taldeKey = zikloKey + ":" + t;
            if (hautatutakoTaldeak.contains(taldeKey)) {
                emaitza.add(t);
            }
        }
        if (!emaitza.isEmpty()) {
            return emaitza;
        }
        if (hautatutakoZikloak.contains(zikloKey)) {
            for (int t = 0; t < safe(zikloaDto.taldeak()).size(); t++) {
                emaitza.add(t);
            }
        }
        return emaitza;
    }

    private Familia ensureFamilia(SeedFamiliaDto dto, Kontagailuak k) {
        Optional<Familia> existing = bilatuFamilia(dto.izena(), dto.slug());
        if (existing.isPresent()) {
            k.lehendikZeudenak++;
            return existing.get();
        }
        Familia familia = Familia.builder()
                .izena(dto.izena())
                .slug(dto.slug())
                .aktibo(dto.aktibo() == null || dto.aktibo())
                .build();
        k.sortutakoFamiliak++;
        return familiaRepository.save(familia);
    }

    private Zikloa ensureZikloa(SeedZikloaDto dto, Familia familia, Kontagailuak k) {
        Optional<Zikloa> existing = zikloaRepository.findByIzenaIgnoreCaseAndFamilia(dto.izena(), familia);
        if (existing.isPresent()) {
            k.lehendikZeudenak++;
            return existing.get();
        }
        Zikloa zikloa = new Zikloa();
        zikloa.setIzena(dto.izena());
        zikloa.setMaila(ZikloMaila.valueOf(dto.maila()));
        zikloa.setFamilia(familia);
        k.sortutakoZikloak++;
        return zikloaRepository.save(zikloa);
    }

    private void ensureModuluak(SeedTaldeaDto taldeaDto, Taldea taldea, Map<String, SeedMailaDto> mailak, Kontagailuak k) {
        for (SeedModuloaDto moduloaDto : safe(taldeaDto.moduluak())) {
            Maila maila = ensureMaila(moduloaDto.mailaKodea(), mailak, k);
            if (moduloaRepository.findByKodeaIgnoreCaseAndEeiKodeaIgnoreCaseAndTaldeaAndMaila(
                    moduloaDto.kodea(), moduloaDto.eeiKodea(), taldea, maila).isPresent()) {
                k.lehendikZeudenak++;
                continue;
            }
            Moduloa moduloa = new Moduloa();
            moduloa.setIzena(moduloaDto.izena());
            moduloa.setKodea(moduloaDto.kodea());
            moduloa.setEeiKodea(moduloaDto.eeiKodea());
            moduloa.setOrduak(moduloaDto.orduak());
            moduloa.setDualOrduak(moduloaDto.dualOrduak());
            moduloa.setHautazkoa(Boolean.TRUE.equals(moduloaDto.hautazkoa()));
            moduloa.setMaila(maila);
            moduloa.setTaldea(taldea);
            moduloaRepository.save(moduloa);
            k.sortutakoModuluak++;
        }
    }

    private Maila ensureMaila(String mailaKodea, Map<String, SeedMailaDto> mailak, Kontagailuak k) {
        Optional<Maila> existing = mailaRepository.findByKodeaIgnoreCase(mailaKodea);
        if (existing.isPresent()) {
            k.lehendikZeudenak++;
            return existing.get();
        }
        SeedMailaDto dto = mailak.get(norm(mailaKodea));
        if (dto == null) {
            throw new IllegalArgumentException("Ez da mailarik aurkitu JSONean kode honentzat: " + mailaKodea);
        }
        Maila maila = new Maila();
        maila.setKodea(dto.kodea());
        maila.setIzena(dto.izena());
        maila.setOrdena(dto.ordena());
        maila.setAktibo(dto.aktibo() == null || dto.aktibo());
        k.sortutakoMailak++;
        return mailaRepository.save(maila);
    }

    private Optional<Familia> bilatuFamilia(String izena, String slug) {
        Optional<Familia> byIzena = familiaRepository.findByIzenaIgnoreCase(izena);
        if (byIzena.isPresent()) {
            return byIzena;
        }
        if (slug != null && !slug.isBlank()) {
            return familiaRepository.findBySlugIgnoreCase(slug);
        }
        return Optional.empty();
    }

    private SeedKatalogoaDto irakurriKatalogoa() {
        try {
            ClassPathResource resource = new ClassPathResource(KATALOGOA_PATH);
            try (var input = resource.getInputStream()) {
                return objectMapper.readValue(input, SeedKatalogoaDto.class);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Ezin izan da seed katalogoa irakurri: " + KATALOGOA_PATH, e);
        }
    }

    private static String zikloKey(int familiaIndex, int zikloIndex) {
        return familiaIndex + ":" + zikloIndex;
    }

    private static String taldeKey(int familiaIndex, int zikloIndex, int taldeIndex) {
        return zikloKey(familiaIndex, zikloIndex) + ":" + taldeIndex;
    }

    private static String norm(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).trim().toLowerCase(Locale.ROOT);
    }

    private static <T> List<T> safe(List<T> list) {
        return list != null ? list : List.of();
    }

    private static class Kontagailuak {
        int sortutakoMailak;
        int sortutakoFamiliak;
        int sortutakoZikloak;
        int sortutakoTaldeak;
        int sortutakoModuluak;
        int lehendikZeudenak;
    }
}
