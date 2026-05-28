package com.koadernoa.app.objektuak.koadernoak.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.EstatistikakFiltroa;
import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.LaburpenKudeatzaileRow;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunMota;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EzadostasunFitxaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.projection.EbaluazioKodeKopuruaProjection;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.entitateak.MatrikulaEgoera;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstatistikakKudeatzaileService {
	private final EstatistikaEbaluazioanRepository estatistikaRepo;
	private final EzadostasunFitxaRepository ezadostasunFitxaRepository;
    private final MatrikulaRepository matrikulaRepository;
	
	private String nullIfBlank(String s) {
	    return (s == null || s.isBlank()) ? null : s;
	}
	
	public List<EbaluazioKodeKopuruaProjection> beteGabeakEbaluazioKodez(EstatistikakFiltroa f) {
	    return estatistikaRepo.countKalkulatuGabeakKodezAktiboan(
	        f.getFamiliaId(), f.getZikloaId(), f.getTaldeaId(), f.getMailaId()
	    );
	  }

	public Page<EstatistikaEbaluazioan> bilatuOrrikatuta(EstatistikakFiltroa f, Pageable pageable) {
	    return estatistikaRepo.bilatuDashboarderako(
	        nullIfBlank(f.getEbaluazioKodea()),
	        f.getKalkulatua(),
	        f.getFamiliaId(),
	        f.getZikloaId(),
	        f.getTaldeaId(),
	        f.getMailaId(),
            nullIfBlank("DENAK".equalsIgnoreCase(f.getEzadostasuna()) ? null : f.getEzadostasuna()),
	        pageable
	    );
	}


	public List<EstatistikaEbaluazioan> bilatuZerrenda(EstatistikakFiltroa f, Sort sort) {
	    return estatistikaRepo.bilatuDashboarderakoZerrenda(
	        nullIfBlank(f.getEbaluazioKodea()),
	        f.getKalkulatua(),
	        f.getFamiliaId(),
	        f.getZikloaId(),
	        f.getTaldeaId(),
	        f.getMailaId(),
            nullIfBlank("DENAK".equalsIgnoreCase(f.getEzadostasuna()) ? null : f.getEzadostasuna()),
	        sort == null || sort.isUnsorted() ? Sort.by("ebaluazioMomentua.ordena").ascending().and(Sort.by("id")) : sort
	    );
	}

    public Page<EzadostasunFitxa> bilatuEzadostasunOrrikatuta(EstatistikakFiltroa f, Pageable pageable) {
        return ezadostasunFitxaRepository.bilatuDashboarderako(
            nullIfBlank(f.getEbaluazioKodea()),
            f.getKalkulatua(),
            f.getFamiliaId(),
            f.getZikloaId(),
            f.getTaldeaId(),
            f.getMailaId(),
            pageable
        );
    }


    // ---- Dropdown datuak: hemen zure zerbitzuetara konektatu (FamiliaService, ZikloaService, TaldeaService, MailaService...)
	public List<Familia> lortuFamiliak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findFamiliaAktiboak();
	}
	public List<Zikloa> lortuZikloak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findZikloAktiboak(f.getFamiliaId());
	}
	public List<Taldea> lortuTaldeak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findTaldeAktiboak(f.getZikloaId());
	}
	public List<Maila> lortuMailak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findMailaAktiboak();
	}
	public List<String> lortuEbaluazioKodeak(EstatistikakFiltroa f) { return List.of("1_EBAL","2_EBAL","3_EBAL","1_FINAL","2_FINAL"); }

    public List<EzadostasunMota> kalkulatuEzadostasunak(EstatistikaEbaluazioan estatistika) {
        List<EzadostasunMota> emaitza = new java.util.ArrayList<>();
        if (estatistika == null || estatistika.getEbaluazioMomentua() == null ||
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig() == null) {
            return emaitza;
        }

        com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig konfig =
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig();

        if (estatistika.getUdPortzentaia() != null &&
                estatistika.getUdPortzentaia() < konfig.getMinBlokePortzentaia()) {
            emaitza.add(EzadostasunMota.UD_EMANDA);
        }
        if (estatistika.getOrduPortzentaia() != null &&
                estatistika.getOrduPortzentaia() < konfig.getMinOrduPortzentaia()) {
            emaitza.add(EzadostasunMota.ORDU_EMANDA);
        }
        if (estatistika.getGaindituPortzentaia() != null &&
                estatistika.getGaindituPortzentaia() < konfig.getMinGaindituPortzentaia()) {
            emaitza.add(EzadostasunMota.GAINDITU);
        }
        if (estatistika.getBertaratzePortzentaia() != null &&
                estatistika.getBertaratzePortzentaia() < konfig.getMinBertaratzePortzentaia()) {
            emaitza.add(EzadostasunMota.BERTARATZE);
        }
        return emaitza;
    }

    public String kalkulatuEzadostasunLabel(EstatistikaEbaluazioan estatistika, EzadostasunMota mota) {
        if (estatistika == null || estatistika.getEbaluazioMomentua() == null ||
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig() == null || mota == null) {
            return "—";
        }
        com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig konfig =
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig();
        return switch (mota) {
            case UD_EMANDA -> "UD-ak emanda < %" + konfig.getMinBlokePortzentaia();
            case ORDU_EMANDA -> "Orduak emanda < %" + konfig.getMinOrduPortzentaia();
            case GAINDITU -> "Gainditu duten ikasleak < %" + konfig.getMinGaindituPortzentaia();
            case BERTARATZE -> "Ikasleen bertaratzea < %" + konfig.getMinBertaratzePortzentaia();
        };
    }

    /*************************** CSV exportazioa egiteko *****************************/
    public void exportCsv(EstatistikakFiltroa f, Sort sort, OutputStream os) throws IOException {

        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {

            // Header
            w.write(String.join(";",
                "ID",
                "Ebaluazioa",
                "EbaluazioKodea",
                "Taldea",
                "Moduloa",
                "Maila",
                "Irakasleak",
                "UD_Emanda",
                "UD_Aurreikusiak",
                "UD_%",
                "Ordu_Emanda",
                "Ordu_Aurreikusiak",
                "Ordu_%",
                "Aprobatuak",
                "Ebaluatuak",
                "Gainditu_%",
                "HutsegiteOrduak",
                "Bertaratze_%",
                "Kalkulatua",
                "AzkenKalkulua"
            ));
            w.newLine();

            int page = 0;
            int size = 1000; // handitu/txikitu nahi baduzu
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while (true) {
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<EstatistikaEbaluazioan> p = bilatuOrrikatuta(f, pageable); // zure metodo bera
                if (p == null || p.isEmpty()) break;

                for (EstatistikaEbaluazioan e : p.getContent()) {
                    String ebIz = (e.getEbaluazioMomentua() != null) ? nz(e.getEbaluazioMomentua().getIzena()) : "";
                    String ebKo = (e.getEbaluazioMomentua() != null) ? nz(e.getEbaluazioMomentua().getKodea()) : "";

                    String taldea = "";
                    String moduloa = "";
                    String maila = "";
                    String irakasleak = "";

                    if (e.getKoadernoa() != null) {
                        if (e.getKoadernoa().getModuloa() != null) {
                            moduloa = nz(e.getKoadernoa().getModuloa().getIzena());
                            if (e.getKoadernoa().getModuloa().getTaldea() != null) {
                                taldea = nz(e.getKoadernoa().getModuloa().getTaldea().getIzena());
                            }
                        }
                        if (e.getKoadernoa().getEgutegia() != null && e.getKoadernoa().getEgutegia().getMaila() != null) {
                            maila = nz(e.getKoadernoa().getEgutegia().getMaila().getIzena());
                        }
                        // zuk HTML-an erabiltzen duzun property bera
                        try {
                            irakasleak = nz(e.getKoadernoa().getIrakasleakLabur());
                        } catch (Exception ex) {
                            irakasleak = "";
                        }
                    }

                    String azken = (e.getAzkenKalkulua() != null) ? e.getAzkenKalkulua().format(fmt) : "";

                    w.write(String.join(";",
                        nz(String.valueOf(e.getId())),
                        csv(ebIz),
                        csv(ebKo),
                        csv(taldea),
                        csv(moduloa),
                        csv(maila),
                        csv(irakasleak),

                        nz(String.valueOf(e.getUnitateakEmanda())),
                        nz(String.valueOf(e.getUnitateakAurreikusiak())),
                        (e.getUdPortzentaia() == null ? "" : String.valueOf(e.getUdPortzentaia())),

                        nz(String.valueOf(e.getOrduakEmanda())),
                        nz(String.valueOf(e.getOrduakAurreikusiak())),
                        (e.getOrduPortzentaia() == null ? "" : String.valueOf(e.getOrduPortzentaia())),

                        nz(String.valueOf(e.getAprobatuak())),
                        nz(String.valueOf(e.getEbaluatuak())),
                        (e.getGaindituPortzentaia() == null ? "" : String.valueOf(e.getGaindituPortzentaia())),

                        nz(String.valueOf(e.getHutsegiteOrduak())),
                        (e.getBertaratzePortzentaia() == null ? "" : String.valueOf(e.getBertaratzePortzentaia())),

                        String.valueOf(e.isKalkulatua()),
                        csv(azken)
                    ));
                    w.newLine();
                }

                w.flush();

                if (!p.hasNext()) break;
                page++;
            }
        }
    }

    public void exportEzadostasunCsv(EstatistikakFiltroa f, Sort sort, OutputStream os) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            w.write(String.join(";",
                "Taldea",
                "Ezadostasun motak",
                "Moduloa",
                "Maila",
                "Ebaluazioa",
                "EbaluazioKodea",
                "EmandakoBlokeak",
                "EmandakoOrduak",
                "Bertaratze_%",
                "Gainditu_%",
                "ZuzentzeJarduerak",
                "ZuzentzeArduraduna",
                "JarraipenData",
                "JarraipenArduradunak",
                "HartutakoErabakiak",
                "ItxieraData",
                "ItxieraArduraduna",
                "EzadostasunaZuzenduta"
            ));
            w.newLine();

            int page = 0;
            int size = 1000;
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            while (true) {
                Pageable pageable = PageRequest.of(page, size, sort);

                Page<EzadostasunFitxa> p = bilatuEzadostasunOrrikatuta(f, pageable);
                if (p == null || p.isEmpty()) break;

                for (EzadostasunFitxa fitxa : p.getContent()) {
                    EstatistikaEbaluazioan e = fitxa.getEstatistika();
                    String taldea = "";
                    String moduloa = "";
                    String maila = "";
                    String ebIz = "";
                    String ebKo = "";

                    if (e != null) {
                        if (e.getKoadernoa() != null) {
                            if (e.getKoadernoa().getModuloa() != null) {
                                moduloa = nz(e.getKoadernoa().getModuloa().getIzena());
                                if (e.getKoadernoa().getModuloa().getTaldea() != null) {
                                    taldea = nz(e.getKoadernoa().getModuloa().getTaldea().getIzena());
                                }
                            }
                            if (e.getKoadernoa().getEgutegia() != null && e.getKoadernoa().getEgutegia().getMaila() != null) {
                                maila = nz(e.getKoadernoa().getEgutegia().getMaila().getIzena());
                            }
                        }
                        if (e.getEbaluazioMomentua() != null) {
                            ebIz = nz(e.getEbaluazioMomentua().getIzena());
                            ebKo = nz(e.getEbaluazioMomentua().getKodea());
                        }
                    }

                    List<String> motak = new java.util.ArrayList<>();
                    if (fitxa.getMota() != null) {
                        motak.add(kalkulatuEzadostasunLabel(e, fitxa.getMota()));
                    } else {
                        List<EzadostasunMota> kalkulatuak = kalkulatuEzadostasunak(e);
                        if (kalkulatuak.isEmpty()) {
                            motak.add("—");
                        } else {
                            for (EzadostasunMota mota : kalkulatuak) {
                                motak.add(kalkulatuEzadostasunLabel(e, mota));
                            }
                        }
                    }
                    String jarraipenData = fitxa.getJarraipenData() != null ? fitxa.getJarraipenData().format(dateFmt) : "";
                    String itxieraData = fitxa.getItxieraData() != null ? fitxa.getItxieraData().format(dateFmt) : "";
                    String bertaratzea = fitxa.getIkasleenBertaratzePortzentaia() != null
                            ? String.valueOf(fitxa.getIkasleenBertaratzePortzentaia())
                            : "";
                    String gainditu = fitxa.getGaindituPortzentaia() != null
                            ? String.valueOf(fitxa.getGaindituPortzentaia())
                            : "";

                    for (String mota : motak) {
                        w.write(String.join(";",
                            csv(taldea),
                            csv(mota),
                            csv(moduloa),
                            csv(maila),
                            csv(ebIz),
                            csv(ebKo),
                            nz(String.valueOf(fitxa.getEmandakoBlokeKopurua())),
                            nz(String.valueOf(fitxa.getEmandakoOrduKopurua())),
                            bertaratzea,
                            gainditu,
                            csv(nz(fitxa.getZuzentzeJarduerak())),
                            csv(nz(fitxa.getZuzentzeJarduerakArduraduna())),
                            csv(jarraipenData),
                            csv(nz(fitxa.getJarraipenArduradunak())),
                            csv(nz(fitxa.getHartutakoErabakiak())),
                            csv(itxieraData),
                            csv(nz(fitxa.getItxieraArduraduna())),
                            csv(fitxa.getEzadostasunaZuzenduta() == null ? "" : (fitxa.getEzadostasunaZuzenduta() ? "Bai" : "Ez"))
                        ));
                        w.newLine();
                    }
                }

                w.flush();

                if (!p.hasNext()) break;
                page++;
            }
        }
    }

    public Page<LaburpenKudeatzaileRow> bilatuLaburpenakOrrikatuta(EstatistikakFiltroa f, Pageable pageable) {
        List<LaburpenKudeatzaileRow> guztiak = bilatuLaburpenakZerrenda(f, pageable.getSort());
        int from = (int) pageable.getOffset();
        if (from >= guztiak.size()) {
            return new org.springframework.data.domain.PageImpl<>(List.of(), pageable, guztiak.size());
        }
        int to = Math.min(from + pageable.getPageSize(), guztiak.size());
        return new org.springframework.data.domain.PageImpl<>(guztiak.subList(from, to), pageable, guztiak.size());
    }

    public List<LaburpenKudeatzaileRow> bilatuLaburpenakZerrenda(EstatistikakFiltroa f, Sort sort) {
        Sort baseSort = (sort == null || sort.isUnsorted())
                ? Sort.by("taldea").ascending().and(Sort.by("moduloa")).and(Sort.by("koadernoId"))
                : sort;

        List<EstatistikaEbaluazioan> guztiak = estatistikaRepo.bilatuDashboarderakoZerrenda(
                null, null, f.getFamiliaId(), f.getZikloaId(), f.getTaldeaId(), f.getMailaId(), null, Sort.by("id"));

        Map<Long, EstatistikaEbaluazioan> lehenFinalaByKoadernoa = new HashMap<>();
        Map<Long, EstatistikaEbaluazioan> bigarrenFinalaByKoadernoa = new HashMap<>();

        for (EstatistikaEbaluazioan e : guztiak) {
            if (e.getKoadernoa() == null || e.getKoadernoa().getId() == null || e.getEbaluazioMomentua() == null) continue;
            String kodea = e.getEbaluazioMomentua().getKodea();
            if ("1_FINAL".equalsIgnoreCase(kodea)) {
                lehenFinalaByKoadernoa.put(e.getKoadernoa().getId(), e);
            } else if ("2_FINAL".equalsIgnoreCase(kodea)) {
                bigarrenFinalaByKoadernoa.put(e.getKoadernoa().getId(), e);
            }
        }

        List<LaburpenKudeatzaileRow> rows = new java.util.ArrayList<>();
        for (Map.Entry<Long, EstatistikaEbaluazioan> entry : lehenFinalaByKoadernoa.entrySet()) {
            Long koadernoId = entry.getKey();
            EstatistikaEbaluazioan lehenFinala = entry.getValue();
            EstatistikaEbaluazioan bigarrenFinala = bigarrenFinalaByKoadernoa.get(koadernoId);
            boolean kalkulatua = bigarrenFinala != null && bigarrenFinala.isKalkulatua();
            if (f.getKalkulatua() != null && !Objects.equals(f.getKalkulatua(), kalkulatua)) {
                continue;
            }

            Long lehenFinalMomentuId = lehenFinala.getEbaluazioMomentua() != null ? lehenFinala.getEbaluazioMomentua().getId() : null;
            Long bigarrenFinalMomentuId = bigarrenFinala != null && bigarrenFinala.getEbaluazioMomentua() != null
                    ? bigarrenFinala.getEbaluazioMomentua().getId() : null;

            int ebaluatuak = 0;
            int aprobatuak = 0;
            List<Matrikula> matrikulak = matrikulaRepository.findByKoadernoa_IdAndEgoera(koadernoId, MatrikulaEgoera.MATRIKULATUA);
            for (Matrikula m : matrikulak) {
                EbaluazioNota n1 = lortuNotaMomentuan(m, lehenFinalMomentuId);
                EbaluazioNota n2 = lortuNotaMomentuan(m, bigarrenFinalMomentuId);
                if (daNotaEdoEgoeraEbaluatua(n1) || daNotaEdoEgoeraEbaluatua(n2)) ebaluatuak++;
                if (daNotaGainditua(n1) || daNotaGainditua(n2)) aprobatuak++;
            }

            LocalDateTime azkenKalkulua = (bigarrenFinala != null && bigarrenFinala.getAzkenKalkulua() != null)
                    ? bigarrenFinala.getAzkenKalkulua()
                    : lehenFinala.getAzkenKalkulua();

            rows.add(new LaburpenKudeatzaileRow(
                    koadernoId,
                    lehenFinala.getKoadernoa() != null && lehenFinala.getKoadernoa().getModuloa() != null
                            && lehenFinala.getKoadernoa().getModuloa().getTaldea() != null
                            ? nz(lehenFinala.getKoadernoa().getModuloa().getTaldea().getIzena()) : "",
                    lehenFinala.getKoadernoa() != null && lehenFinala.getKoadernoa().getModuloa() != null
                            ? nz(lehenFinala.getKoadernoa().getModuloa().getIzena()) : "",
                    lehenFinala.getKoadernoa() != null && lehenFinala.getKoadernoa().getEgutegia() != null
                            && lehenFinala.getKoadernoa().getEgutegia().getMaila() != null
                            ? nz(lehenFinala.getKoadernoa().getEgutegia().getMaila().getIzena()) : "",
                    lehenFinala.getKoadernoa() != null ? nz(lehenFinala.getKoadernoa().getIrakasleakLabur()) : "",
                    lehenFinala.getUnitateakEmanda(),
                    lehenFinala.getUnitateakAurreikusiak(),
                    lehenFinala.getOrduakEmanda(),
                    lehenFinala.getOrduakAurreikusiak(),
                    aprobatuak,
                    ebaluatuak,
                    lehenFinala.getBertaratzeOinarriOrduak(),
                    lehenFinala.getHutsegiteOrduak(),
                    kalkulatua,
                    azkenKalkulua
            ));
        }

        rows.sort(buildLaburpenComparator(baseSort));
        return rows;
    }

    public void exportLaburpenCsv(EstatistikakFiltroa f, Sort sort, OutputStream os) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
            w.write(String.join(";",
                    "KoadernoId","Taldea","Moduloa","Maila","Irakasleak",
                    "UD_Emanda","UD_Aurreikusiak","UD_%",
                    "Ordu_Emanda","Ordu_Aurreikusiak","Ordu_%",
                    "Aprobatuak","Ebaluatuak","Gainditu_%",
                    "HutsegiteOrduak","Bertaratze_%","Kalkulatua","AzkenKalkulua"));
            w.newLine();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            for (LaburpenKudeatzaileRow r : bilatuLaburpenakZerrenda(f, sort)) {
                w.write(String.join(";",
                        nz(String.valueOf(r.getKoadernoId())),
                        csv(r.getTaldea()),
                        csv(r.getModuloa()),
                        csv(r.getMaila()),
                        csv(r.getIrakasleak()),
                        String.valueOf(r.getUnitateakEmanda()),
                        String.valueOf(r.getUnitateakAurreikusiak()),
                        r.getUdPortzentaia() == null ? "" : String.valueOf(r.getUdPortzentaia()),
                        String.valueOf(r.getOrduakEmanda()),
                        String.valueOf(r.getOrduakAurreikusiak()),
                        r.getOrduPortzentaia() == null ? "" : String.valueOf(r.getOrduPortzentaia()),
                        String.valueOf(r.getAprobatuak()),
                        String.valueOf(r.getEbaluatuak()),
                        r.getGaindituPortzentaia() == null ? "" : String.valueOf(r.getGaindituPortzentaia()),
                        String.valueOf(r.getHutsegiteOrduak()),
                        r.getBertaratzePortzentaia() == null ? "" : String.valueOf(r.getBertaratzePortzentaia()),
                        String.valueOf(r.isKalkulatua()),
                        r.getAzkenKalkulua() == null ? "" : r.getAzkenKalkulua().format(fmt)
                ));
                w.newLine();
            }
            w.flush();
        }
    }

    private Comparator<LaburpenKudeatzaileRow> buildLaburpenComparator(Sort sort) {
        Comparator<LaburpenKudeatzaileRow> comparator = Comparator.comparing(
                LaburpenKudeatzaileRow::getKoadernoId, Comparator.nullsLast(Long::compareTo));
        for (Sort.Order order : sort) {
            Comparator<LaburpenKudeatzaileRow> next = switch (order.getProperty()) {
                case "taldea" -> Comparator.comparing(LaburpenKudeatzaileRow::getTaldea, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "moduloa" -> Comparator.comparing(LaburpenKudeatzaileRow::getModuloa, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "maila" -> Comparator.comparing(LaburpenKudeatzaileRow::getMaila, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                case "kalkulatua" -> Comparator.comparing(LaburpenKudeatzaileRow::isKalkulatua);
                case "azkenKalkulua" -> Comparator.comparing(LaburpenKudeatzaileRow::getAzkenKalkulua, Comparator.nullsLast(LocalDateTime::compareTo));
                case "gaindituPortzentaia" -> Comparator.comparing(LaburpenKudeatzaileRow::getGaindituPortzentaia, Comparator.nullsLast(Integer::compareTo));
                default -> Comparator.comparing(LaburpenKudeatzaileRow::getKoadernoId, Comparator.nullsLast(Long::compareTo));
            };
            if (order.getDirection().isDescending()) next = next.reversed();
            comparator = next.thenComparing(comparator);
        }
        return comparator;
    }

    private EbaluazioNota lortuNotaMomentuan(Matrikula matrikula, Long momentuId) {
        if (matrikula == null || momentuId == null || matrikula.getNotak() == null) return null;
        return matrikula.getNotak().stream()
                .filter(n -> n.getEbaluazioMomentua() != null
                        && Objects.equals(n.getEbaluazioMomentua().getId(), momentuId))
                .findFirst()
                .orElse(null);
    }

    private boolean daNotaGainditua(EbaluazioNota nota) {
        return nota != null && nota.getNota() != null && nota.getNota() >= 5.0;
    }

    private boolean daNotaEdoEgoeraEbaluatua(EbaluazioNota nota) {
        if (nota == null) return false;
        if (nota.getNota() != null && nota.getNota() >= 1.0 && nota.getNota() <= 10.0) return true;
        return nota.getEgoera() != null && nota.getEgoera().isEbaluatua();
    }

    private String nz(String s) { return s == null ? "" : s; }

    /**
     * CSV-eko field-a seguru idazteko:
     * - ; edo " edo \n badu, komatxo artean sartu
     * - " bada, "" bihurtu
     */
    private String csv(String s) {
        if (s == null) return "";
        boolean mustQuote = s.contains(";") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        String v = s.replace("\"", "\"\"");
        return mustQuote ? ("\"" + v + "\"") : v;
    }
}
