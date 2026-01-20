package com.koadernoa.app.objektuak.koadernoak.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.EstatistikakFiltroa;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.EzadostasunFitxaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.projection.EbaluazioKodeKopuruaProjection;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstatistikakKudeatzaileService {
	private final EstatistikaEbaluazioanRepository estatistikaRepo;
	private final EzadostasunFitxaRepository ezadostasunFitxaRepository;
	
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
	        pageable
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

    public List<String> kalkulatuEzadostasunak(EstatistikaEbaluazioan estatistika) {
        List<String> emaitza = new java.util.ArrayList<>();
        if (estatistika == null || estatistika.getEbaluazioMomentua() == null ||
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig() == null) {
            return emaitza;
        }

        com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig konfig =
                estatistika.getEbaluazioMomentua().getEzadostasunKonfig();

        if (estatistika.getUdPortzentaia() != null &&
                estatistika.getUdPortzentaia() < konfig.getMinBlokePortzentaia()) {
            emaitza.add("UD-ak emanda < %" + konfig.getMinBlokePortzentaia());
        }
        if (estatistika.getOrduPortzentaia() != null &&
                estatistika.getOrduPortzentaia() < konfig.getMinOrduPortzentaia()) {
            emaitza.add("Orduak emanda < %" + konfig.getMinOrduPortzentaia());
        }
        if (estatistika.getGaindituPortzentaia() != null &&
                estatistika.getGaindituPortzentaia() < konfig.getMinGaindituPortzentaia()) {
            emaitza.add("Gainditu duten ikasleak < %" + konfig.getMinGaindituPortzentaia());
        }
        if (estatistika.getBertaratzePortzentaia() != null &&
                estatistika.getBertaratzePortzentaia() < konfig.getMinBertaratzePortzentaia()) {
            emaitza.add("Ikasleen bertaratzea < %" + konfig.getMinBertaratzePortzentaia());
        }
        return emaitza;
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

                    String motak = String.join(", ", kalkulatuEzadostasunak(e));
                    String jarraipenData = fitxa.getJarraipenData() != null ? fitxa.getJarraipenData().format(dateFmt) : "";
                    String itxieraData = fitxa.getItxieraData() != null ? fitxa.getItxieraData().format(dateFmt) : "";
                    String bertaratzea = fitxa.getIkasleenBertaratzePortzentaia() != null
                            ? String.valueOf(fitxa.getIkasleenBertaratzePortzentaia())
                            : "";
                    String gainditu = fitxa.getGaindituPortzentaia() != null
                            ? String.valueOf(fitxa.getGaindituPortzentaia())
                            : "";

                    w.write(String.join(";",
                        csv(taldea),
                        csv(motak),
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

                w.flush();

                if (!p.hasNext()) break;
                page++;
            }
        }
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
