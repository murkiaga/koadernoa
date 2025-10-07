package com.koadernoa.app.objektuak.koadernoak.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Egutegia;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.egutegia.repository.EgutegiaRepository;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EbaluazioMota;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Jarduera;
import com.koadernoa.app.objektuak.koadernoak.entitateak.JardueraSortuDto;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.KoadernoaSortuDto;
import com.koadernoa.app.objektuak.koadernoak.repository.JardueraRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.KoadernoaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.modulua.repository.ModuloaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KoadernoaService {

    private final ModuloaRepository moduloaRepository;
    private final EgutegiaRepository egutegiaRepository;
    private final IrakasleaRepository irakasleaRepository;
    private final KoadernoaRepository koadernoaRepository;
    private final JardueraRepository jardueraRepository;

    public void sortuKoadernoakIkasturteBerrirako(Ikasturtea ikasturtea) {
        for (Egutegia egutegia : ikasturtea.getEgutegiak()) {
            Maila maila = egutegia.getMaila();
            List<Moduloa> moduluak = moduloaRepository.findByMaila_Id(maila.getId());
            for (Moduloa moduloa : moduluak) {
                Koadernoa koadernoa = new Koadernoa();
                koadernoa.setModuloa(moduloa);
                koadernoa.setEgutegia(egutegia);
                koadernoaRepository.save(koadernoa);
            }
        }
    }

    public Koadernoa findById(Long id) {
        return koadernoaRepository.findById(id).orElseThrow();
    }

    public List<Koadernoa> findByIrakaslea(Irakaslea irakaslea) {
        return koadernoaRepository.findByIrakasleakContaining(irakaslea);
    }

    public List<Moduloa> lortuErabilgarriDaudenModuluak(Irakaslea irakaslea) {
        return moduloaRepository.findByTaldea_Zikloa_Familia(irakaslea.getMintegia());
    }

    public List<Egutegia> lortuEgutegiGuztiak() {
        return egutegiaRepository.findAll();
    }

    public List<Irakaslea> lortuFamiliaBerekoIrakasleak(Irakaslea irakaslea) {
        return irakasleaRepository.findByMintegia(irakaslea.getMintegia())
            .stream().filter(i -> !i.getId().equals(irakaslea.getId()))
            .toList();
    }
    

    @Transactional
    public Koadernoa sortuKoadernoa(KoadernoaSortuDto dto, Irakaslea irakaslea) {
        Moduloa moduloa = moduloaRepository.findById(dto.getModuloaId())
            .orElseThrow(() -> new IllegalArgumentException("Ez da modulu hori aurkitu."));

        Egutegia egutegia = egutegiaRepository
                .findByIkasturtea_AktiboaTrueAndMaila_Id(moduloa.getMaila().getId())
                .orElseThrow(() -> new IllegalStateException(
                    "Ez da egutegirik aurkitu aktibo dagoen ikasturterako eta maila horretarako."));

        if (!moduloa.getTaldea().getZikloa().getFamilia().equals(irakaslea.getMintegia())) {
            throw new AccessDeniedException("Beste familiako modulua aukeratu da.");
        }
        if (!java.util.Objects.equals(moduloa.getMaila().getId(), egutegia.getMaila().getId())) {
            throw new IllegalArgumentException("Moduluaren eta egutegiaren maila ez datoz bat.");
        }

        List<Irakaslea> irakasleak = new ArrayList<>();
        irakasleak.add(irakaslea);

        if (dto.getIrakasleIdZerrenda() != null && !dto.getIrakasleIdZerrenda().isEmpty()) {
            List<Irakaslea> besteIrakasleak = irakasleaRepository.findAllById(dto.getIrakasleIdZerrenda());
            for (Irakaslea i : besteIrakasleak) {
                if (!i.getId().equals(irakaslea.getId())) irakasleak.add(i);
            }
        }

        Koadernoa k = new Koadernoa();
        k.setModuloa(moduloa);
        k.setEgutegia(egutegia);
        k.setIrakasleak(irakasleak);
        k.setJarduerak(List.of());
        k.setNotaFitxategiak(List.of());
        k.setEstatistikak(new ArrayList<>());

        List<EstatistikaEbaluazioan> estatistikak = sortuEstatistikak(k);
        k.setEstatistikak(estatistikak);

        return koadernoaRepository.save(k);
    }

    @Transactional
    private List<EstatistikaEbaluazioan> sortuEstatistikak(Koadernoa koadernoa) {
    	Egutegia eg = koadernoa.getEgutegia();

        List<EbaluazioMota> ebaluazioMotak = new ArrayList<>();
        // Beti 1. ebaluazioa
        ebaluazioMotak.add(EbaluazioMota.LEHENENGO_EBALUAZIOA);

        // 2. ebaluazioa definituta badago egutegian, gehitu
        if (eg.getBigarrenEbalBukaera() != null) {
            ebaluazioMotak.add(EbaluazioMota.BIGARREN_EBALUAZIOA);
        }

        // Bi finalak beti
        ebaluazioMotak.add(EbaluazioMota.LEHENENGO_FINALA);
        ebaluazioMotak.add(EbaluazioMota.BIGARREN_FINALA);

        return ebaluazioMotak.stream().map(mota -> {
            EstatistikaEbaluazioan e = new EstatistikaEbaluazioan();
            e.setEbaluazioMota(mota);
            e.setKoadernoa(koadernoa);
            return e;
        }).toList();
    }

    /** INSERT segurua: Koadernoa referentziaz (managed) lotu */
    @Transactional
    public void gordeJarduera(Koadernoa koadernoa, JardueraSortuDto dto) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalStateException("Koaderno aktiborik gabe ezin da jarduera gorde");
        }
        Jarduera j = new Jarduera();

        // REFERENTZIAZ: ez pasatu objektua transiente/detached egoeran
        Koadernoa ref = koadernoaRepository.getReferenceById(koadernoa.getId());
        j.setKoadernoa(ref);

        j.setTitulua(dto.getTitulua());
        j.setDeskribapena(dto.getDeskribapena());
        j.setData(dto.getData());
        j.setOrduak(dto.getOrduak());
        j.setMota(dto.getMota());

        jardueraRepository.save(j);
    }

    /** UPDATE segurua: koadernoaren jabetza egiaztatu eta gorde */
    @Transactional
    public void eguneratuJarduera(Koadernoa koadernoa, Long id, JardueraSortuDto dto) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalStateException("Koaderno aktiborik gabe ezin da jarduera eguneratu");
        }
        Jarduera j = jardueraRepository.findByIdAndKoadernoaId(id, koadernoa.getId());
        if (j == null) {
            throw new IllegalStateException("Ez da jarduera aurkitu edo ez dagokizu");
        }

        j.setTitulua(dto.getTitulua());
        j.setDeskribapena(dto.getDeskribapena());
        j.setData(dto.getData());
        j.setOrduak(dto.getOrduak());
        j.setMota(dto.getMota());

        jardueraRepository.save(j);
    }

    /** QUERY-ak ID bidez, Detached/Transient saihesteko */
    public List<Jarduera> lortuJarduerakDataTartean(Koadernoa koadernoa, LocalDate hasiera, LocalDate amaiera) {
        if (koadernoa == null || koadernoa.getId() == null) return List.of();
        return jardueraRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
            koadernoa.getId(), hasiera, amaiera
        );
    }

    public Jarduera lortuJardueraKoadernoan(Koadernoa koadernoa, Long id) {
        if (koadernoa == null || koadernoa.getId() == null) return null;
        return jardueraRepository.findByIdAndKoadernoaId(id, koadernoa.getId());
    }
    
    @Transactional
    public void ezabatuJarduera(Koadernoa koadernoa, Long jardueraId) {
        if (koadernoa == null || koadernoa.getId() == null) {
            throw new IllegalArgumentException("Koaderno aktiborik ez.");
        }

        // Zure sinadurarekin: entity edo null
        Jarduera jarduera = jardueraRepository.findByIdAndKoadernoaId(jardueraId, koadernoa.getId());
        if (jarduera == null) {
            throw new IllegalArgumentException("Jarduera ez da existitzen edo ez dagokio koaderno honi.");
        }

        jardueraRepository.delete(jarduera);
    }
    
    @Transactional(readOnly = true)
    public boolean irakasleakBadaukaSarbidea(Irakaslea irakaslea, Koadernoa koadernoa) {
        if (irakaslea == null || irakaslea.getId() == null) return false;
        if (koadernoa == null || koadernoa.getId() == null) return false;
        return koadernoaRepository.existsByIdAndIrakasleak_Id(koadernoa.getId(), irakaslea.getId());
    }
    
    @Transactional(readOnly = true)
    public boolean irakasleakBadaukaSarbidea(Irakaslea irakaslea, Long koadernoId) {
        if (irakaslea == null || irakaslea.getId() == null) return false;
        if (koadernoId == null) return false;
        return koadernoaRepository.existsByIdAndIrakasleak_Id(koadernoId, irakaslea.getId());
    }
}
