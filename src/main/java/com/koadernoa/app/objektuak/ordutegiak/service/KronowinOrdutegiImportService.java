package com.koadernoa.app.objektuak.ordutegiak.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.egutegia.entitateak.Astegunak;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;
import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;
import com.koadernoa.app.objektuak.ordutegiak.repository.IrakasleOrdutegiaRepository;
import com.koadernoa.app.objektuak.ordutegiak.service.KronowinOrdutegiXmlParser.KronowinData;
import com.koadernoa.app.objektuak.ordutegiak.service.KronowinOrdutegiXmlParser.KronowinIrakaslea;
import com.koadernoa.app.objektuak.ordutegiak.service.KronowinOrdutegiXmlParser.KronowinSolucf;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KronowinOrdutegiImportService {
    private static final String JATORRIA = "KRONOWIN";
    private static final String ZERO_ARRAZOIA = "DIA edo HORA 0 da; ez dago asteko ordutegi arruntean kokatzeko modurik";

    private final KronowinOrdutegiXmlParser parser;
    private final IkasturteaRepository ikasturteaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final TaldeaRepository taldeaRepository;
    private final IrakasleOrdutegiaRepository irakasleOrdutegiaRepository;

    public OrdutegiImportResult aurrebista(MultipartFile file, Long ikasturteaId) {
        return prozesatu(file, ikasturteaId, false);
    }

    @Transactional
    public OrdutegiImportResult inportatu(MultipartFile file, Long ikasturteaId) {
        return prozesatu(file, ikasturteaId, true);
    }

    private OrdutegiImportResult prozesatu(MultipartFile file, Long ikasturteaId, boolean gorde) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("XML fitxategia aukeratu behar da");
        }
        Ikasturtea ikasturtea = ikasturteaRepository.findById(ikasturteaId)
                .orElseThrow(() -> new IllegalArgumentException("Ikasturtea ez da aurkitu: " + ikasturteaId));
        KronowinData data;
        try {
            data = parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Ezin izan da fitxategia irakurri", e);
        }

        if (gorde) {
            irakasleOrdutegiaRepository.deleteByIkasturteaId(ikasturtea.getId());
        }

        OrdutegiImportResult result = new OrdutegiImportResult();
        Map<String, IrakasleOrdutegia> ordutegiak = new HashMap<>();
        Map<String, Irakaslea> irakasleCache = new HashMap<>();
        Map<String, Taldea> taldeCache = new HashMap<>();

        for (KronowinSolucf solucf : data.solucf()) {
            result.gehituSolucf();
            if (isZero(solucf.dia()) || isZero(solucf.hora())) {
                result.gehituSaltatutakoa(new OrdutegiImportResult.SaltatutakoLerroa(
                        solucf.prof(), solucf.asig(), solucf.aula(), solucf.codgrupo(), solucf.dia(), solucf.hora(), ZERO_ARRAZOIA));
                continue;
            }
            Astegunak asteguna = mapAsteguna(solucf.dia());
            if (asteguna == null || solucf.hora() == null) {
                result.gehituSaltatutakoa(new OrdutegiImportResult.SaltatutakoLerroa(
                        solucf.prof(), solucf.asig(), solucf.aula(), solucf.codgrupo(), solucf.dia(), solucf.hora(),
                        "DIA edo HORA baliogabea da"));
                continue;
            }

            String profKey = key(solucf.prof());
            KronowinIrakaslea xmlIrakaslea = data.irakasleak().get(profKey);
            String xmlEmail = xmlIrakaslea != null ? xmlIrakaslea.email() : "";
            String emailKey = key(xmlEmail);
            Irakaslea irakaslea = isBlank(xmlEmail) ? null : irakasleCache.computeIfAbsent(emailKey,
                    k -> irakasleaRepository.findByEmailaIgnoreCase(xmlEmail).orElse(null));
            if (irakaslea == null) {
                result.gehituLotuGabekoIrakaslea(solucf.prof(), xmlIrakaslea != null ? xmlIrakaslea.izena() : "", xmlEmail);
                continue;
            }

            String moduluIzena = data.moduluIzenak().getOrDefault(key(solucf.asig()), "");

            Taldea taldea = null;
            if (!isBlank(solucf.codgrupo())) {
                taldea = taldeCache.computeIfAbsent(key(solucf.codgrupo()),
                        k -> taldeaRepository.findByIzenaIgnoreCase(solucf.codgrupo()).orElse(null));
                if (taldea == null) {
                    result.gehituLotuGabekoTaldea(solucf.codgrupo());
                }
            }

            if (gorde) {
                IrakasleOrdutegia ordutegia = ordutegiak.computeIfAbsent(emailKey, k -> sortuOrdutegia(ikasturtea, irakaslea, solucf.prof(), xmlIrakaslea));
                ordutegia.gehituLerroa(sortuLerroa(solucf, asteguna, taldea, moduluIzena,
                        data.gelaIzenak().getOrDefault(key(solucf.aula()), "")));
            }
            result.gehituSortutakoLerroa();
        }

        if (gorde) {
            irakasleOrdutegiaRepository.saveAll(ordutegiak.values());
        }
        return result;
    }

    private IrakasleOrdutegia sortuOrdutegia(Ikasturtea ikasturtea, Irakaslea irakaslea, String prof, KronowinIrakaslea xmlIrakaslea) {
        IrakasleOrdutegia ordutegia = new IrakasleOrdutegia();
        ordutegia.setIkasturtea(ikasturtea);
        ordutegia.setIrakaslea(irakaslea);
        ordutegia.setXmlIrakasleKodea(prof);
        ordutegia.setXmlIrakasleIzena(xmlIrakaslea != null ? xmlIrakaslea.izena() : null);
        ordutegia.setJatorria(JATORRIA);
        ordutegia.setInportazioData(LocalDateTime.now());
        return ordutegia;
    }

    private IrakasleOrdutegiLerroa sortuLerroa(KronowinSolucf solucf, Astegunak asteguna,
                                               Taldea taldea, String moduluIzena, String gelaIzena) {
        IrakasleOrdutegiLerroa lerroa = new IrakasleOrdutegiLerroa();
        lerroa.setAsteguna(asteguna);
        lerroa.setOrduZenbakia(solucf.hora());
        lerroa.setSaioKopurua(solucf.sesiones());
        lerroa.setModuluKodea(solucf.asig());
        lerroa.setModuluIzena(moduluIzena);
        lerroa.setTaldeKodea(solucf.codgrupo());
        lerroa.setTaldea(taldea);
        lerroa.setGelaKodea(solucf.aula());
        lerroa.setGelaIzena(gelaIzena);
        lerroa.setCurso(solucf.curso());
        lerroa.setGrupo(solucf.grupo());
        lerroa.setNivel(solucf.nivel());
        lerroa.setTurno(solucf.turno());
        lerroa.setMarco(solucf.marco());
        lerroa.setTarea(solucf.tarea());
        return lerroa;
    }

    private Astegunak mapAsteguna(Integer dia) {
        if (dia == null) return null;
        return switch (dia) {
            case 1 -> Astegunak.ASTELEHENA;
            case 2 -> Astegunak.ASTEARTEA;
            case 3 -> Astegunak.ASTEAZKENA;
            case 4 -> Astegunak.OSTEGUNA;
            case 5 -> Astegunak.OSTIRALA;
            default -> null;
        };
    }

    private boolean isZero(Integer value) {
        return value != null && value == 0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String key(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
