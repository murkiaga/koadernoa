package com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioMomentuaRepository;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.jokabidea.service.IkasleEgunJardueraService;
import com.koadernoa.app.objektuak.jokabidea.service.IkasleEgunJardueraService.JokabideLaburpena;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltaIkasleRow;
import com.koadernoa.app.objektuak.koadernoak.entitateak.denboralizazioa.FaltakBistaDTO;
import com.koadernoa.app.objektuak.koadernoak.service.DenboralizazioFaltaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.repository.TaldeaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IkasleakKontsultatuService {

    private final IkasleaRepository ikasleaRepository;
    private final MatrikulaRepository matrikulaRepository;
    private final IkasturteaRepository ikasturteaRepository;
    private final JokabideDesegokiaRepository jokabideDesegokiaRepository;
    private final TaldeaRepository taldeaRepository;
    private final EbaluazioMomentuaRepository ebaluazioMomentuaRepository;
    private final EbaluazioNotaRepository ebaluazioNotaRepository;
    private final DenboralizazioFaltaService denboralizazioFaltaService;
    private final IkasleEgunJardueraService ikasleEgunJardueraService;

    public Page<Ikaslea> bilatu(Long zikloaId, Long taldeaId, int page, int size) {
        int tamaina = Math.min(100, Math.max(20, size));
        int orria = Math.max(0, page);
        return ikasleaRepository.bilatuKudeatzaile(
                zikloaId,
                taldeaId,
                PageRequest.of(orria, tamaina, Sort.by("abizena1", "abizena2", "izena").ascending()));
    }

    public Ikaslea getIkaslea(Long id) {
        return ikasleaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da aurkitu: " + id));
    }

    public List<Ikaslea> bilatuAutocomplete(String q) {
        return ikasleaRepository.bilatuAutocomplete(q, PageRequest.of(0, 10));
    }

    public Taldea getTaldea(Long id) {
        return taldeaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Taldea ez da aurkitu: " + id));
    }

    public List<Ikaslea> getTaldekoIkasleak(Long taldeaId) {
        getTaldea(taldeaId);
        return ikasleaRepository.findByTaldea_IdOrderByAbizena1AscAbizena2AscIzenaAsc(taldeaId);
    }

    public List<Taldea> getIkasleakDituztenTaldeak() {
        return ikasleaRepository.findDistinctTaldeakWithStudents().stream()
                .sorted(Comparator.comparing(Taldea::getIzena,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<Ikasturtea> getIkasturteak(Long ikasleaId) {
        List<Ikasturtea> ikasturteak = new ArrayList<>(matrikulaRepository.findIkasturteakByIkaslea(ikasleaId));
        ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc().ifPresent(aktiboa -> {
            if (ikasturteak.stream().noneMatch(ik -> ik.getId().equals(aktiboa.getId()))) {
                ikasturteak.add(0, aktiboa);
            }
        });
        return ikasturteak;
    }

    public Long aukeratuIkasturtea(Long eskatutakoa, List<Ikasturtea> ikasturteak) {
        if (eskatutakoa != null) {
            return eskatutakoa;
        }
        return ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc()
                .map(Ikasturtea::getId)
                .orElseGet(() -> ikasturteak.isEmpty() ? null : ikasturteak.get(0).getId());
    }

    public List<Matrikula> getMatrikulak(Long ikasleaId, Long ikasturteaId) {
        return matrikulaRepository.findIkaslearenMatrikulakByIkasturtea(ikasleaId, ikasturteaId);
    }

    public IkaslearenEbaluazioDatuak getEbaluazioDatuak(Long ikasleaId, Long ikasturteaId) {
        List<Matrikula> matrikulak = getMatrikulak(ikasleaId, ikasturteaId);
        if (matrikulak.isEmpty()) {
            return new IkaslearenEbaluazioDatuak(List.of(), List.of());
        }

        Map<Long, List<EbaluazioMomentua>> momentuakMailarenArabera = new HashMap<>();
        Map<String, EbaluazioMomentuZutabea> zutabeakKodearenArabera = new LinkedHashMap<>();

        matrikulak.stream()
                .map(Matrikula::getKoadernoa)
                .filter(k -> k != null && k.getModuloa() != null && k.getModuloa().getMaila() != null)
                .map(k -> k.getModuloa().getMaila())
                .filter(maila -> maila.getId() != null)
                .distinct()
                .forEach(maila -> {
                    List<EbaluazioMomentua> momentuak = ebaluazioMomentuaRepository
                            .findByMailaAndAktiboTrueOrderByOrdenaAsc(maila);
                    momentuakMailarenArabera.put(maila.getId(), momentuak);
                    momentuak.forEach(momentua -> zutabeakKodearenArabera.putIfAbsent(
                            momentua.getKodea(),
                            new EbaluazioMomentuZutabea(
                                    momentua.getKodea(), momentua.getIzena(), momentua.getOrdena())));
                });

        List<EbaluazioMomentuZutabea> zutabeak = zutabeakKodearenArabera.values().stream()
                .sorted(Comparator
                        .comparing(EbaluazioMomentuZutabea::getOrdena,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(EbaluazioMomentuZutabea::getKodea,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<Long> matrikulaIds = matrikulak.stream().map(Matrikula::getId).toList();
        Map<Long, Map<String, String>> notak = new HashMap<>();
        for (EbaluazioNota nota : ebaluazioNotaRepository.findByMatrikulaIds(matrikulaIds)) {
            if (nota.getMatrikula() == null || nota.getEbaluazioMomentua() == null) continue;
            notak.computeIfAbsent(nota.getMatrikula().getId(), __ -> new HashMap<>())
                    .put(nota.getEbaluazioMomentua().getKodea(), kalifikazioaBistaratzeko(nota));
        }

        Map<Long, Double> hutsegitePortzentaiak = denboralizazioFaltaService
                .kalkulatuHutsegitePortzentaiak(matrikulak);
        List<IkaslearenMatrikulaLaburpena> laburpenak = matrikulak.stream()
                .map(matrikula -> {
                    Long mailaId = matrikula.getKoadernoa() != null
                            && matrikula.getKoadernoa().getModuloa() != null
                            && matrikula.getKoadernoa().getModuloa().getMaila() != null
                            ? matrikula.getKoadernoa().getModuloa().getMaila().getId()
                            : null;
                    Set<String> momentuKodeak = momentuakMailarenArabera
                            .getOrDefault(mailaId, List.of()).stream()
                            .map(EbaluazioMomentua::getKodea)
                            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
                    return new IkaslearenMatrikulaLaburpena(
                            matrikula,
                            momentuKodeak,
                            notak.getOrDefault(matrikula.getId(), Map.of()),
                            hutsegitePortzentaiak.getOrDefault(matrikula.getId(), 0.0));
                })
                .toList();
        return new IkaslearenEbaluazioDatuak(zutabeak, laburpenak);
    }

    private String kalifikazioaBistaratzeko(EbaluazioNota nota) {
        if (nota.getEgoera() != null) {
            return nota.getEgoera().getIzena() != null && !nota.getEgoera().getIzena().isBlank()
                    ? nota.getEgoera().getIzena()
                    : nota.getEgoera().getKodea();
        }
        String balioa = nota.getNotaBistaratzeko();
        return balioa != null ? balioa : "";
    }

    public record IkaslearenEbaluazioDatuak(
            List<EbaluazioMomentuZutabea> momentuak,
            List<IkaslearenMatrikulaLaburpena> matrikulak) {
    }

    public IkaslearenAsistentziaDatuak getAsistentziaDatuak(
            Long ikasleaId, int urtea, int hilabetea) {
        Ikaslea ikaslea = getIkaslea(ikasleaId);
        Ikasturtea ikasturtea = ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc().orElse(null);
        YearMonth hautatutakoHilabetea = YearMonth.of(urtea, hilabetea);
        String hilabeteUrtea = hautatutakoHilabetea.atDay(1)
                .format(DateTimeFormatter.ofPattern("LLLL yyyy").withLocale(new Locale("eu", "ES")));
        if (ikasturtea == null) {
            return new IkaslearenAsistentziaDatuak(
                    ikaslea, null, urtea, hilabetea, hilabeteUrtea, List.of(), List.of());
        }

        List<Matrikula> matrikulak = matrikulaRepository
                .findMatrikulatuakByIkasleaAndIkasturtea(ikasleaId, ikasturtea.getId());
        LocalDate hilabeteHasiera = hautatutakoHilabetea.atDay(1);
        LocalDate hilabeteAmaiera = hautatutakoHilabetea.atEndOfMonth();
        Set<LocalDate> egunak = new TreeSet<>();
        List<IkaslearenAsistentziaLerroa> lerroak = new ArrayList<>();

        for (Matrikula matrikula : matrikulak) {
            FaltakBistaDTO faltak = denboralizazioFaltaService
                    .kalkulatuMatrikularenFaltenBista(matrikula, hilabetea, urtea);
            FaltaIkasleRow faltaLerroa = faltak.getIkasleRows().stream()
                    .findFirst()
                    .orElseGet(() -> {
                        FaltaIkasleRow hutsa = new FaltaIkasleRow();
                        hutsa.setMatrikula(matrikula);
                        return hutsa;
                    });
            egunak.addAll(faltak.getEgunak());

            Map<LocalDate, String> oharrak = ikasleEgunJardueraService
                    .ikaslearenOharrak(
                            ikasleaId, matrikula.getKoadernoa().getId(), hilabeteHasiera, hilabeteAmaiera);
            Map<LocalDate, List<JokabideLaburpena>> jokabideak = ikasleEgunJardueraService
                    .ikaslearenJokabideak(
                            ikasleaId, matrikula.getKoadernoa().getId(), hilabeteHasiera, hilabeteAmaiera);

            lerroak.add(new IkaslearenAsistentziaLerroa(
                    matrikula,
                    faltak.getProgramaOrduak(),
                    faltaLerroa.getFaltaOrduak(),
                    faltaLerroa.getFaltaPortzentaia(),
                    Map.copyOf(faltaLerroa.getBalioak()),
                    Set.copyOf(faltak.getEgunekoOrduak().keySet()),
                    Map.copyOf(oharrak),
                    Map.copyOf(jokabideak)));
        }

        return new IkaslearenAsistentziaDatuak(
                ikaslea,
                ikasturtea,
                urtea,
                hilabetea,
                hilabeteUrtea,
                List.copyOf(egunak),
                List.copyOf(lerroak));
    }

    public record IkaslearenAsistentziaDatuak(
            Ikaslea ikaslea,
            Ikasturtea ikasturtea,
            int urtea,
            int hilabetea,
            String hilabeteUrtea,
            List<LocalDate> egunak,
            List<IkaslearenAsistentziaLerroa> lerroak) {
    }

    public List<JokabideDesegokia> getJokabideDesegokiak(Long ikasleaId) {
        getIkaslea(ikasleaId);
        return jokabideDesegokiaRepository.findAllByIkasleaIdForKontsulta(ikasleaId);
    }

    public boolean badituJokabideDesegokiak(Long ikasleaId) {
        return jokabideDesegokiaRepository.existsByIkasleaId(ikasleaId);
    }

    public JokabideDesegokia getJokabideDesegokia(Long ikasleaId, Long jokabideaId) {
        JokabideDesegokia jokabidea = jokabideDesegokiaRepository.findById(jokabideaId)
                .orElseThrow(() -> new IllegalArgumentException("Jokabide desegokia ez da aurkitu."));
        if (jokabidea.getIkaslea() == null || !ikasleaId.equals(jokabidea.getIkaslea().getId())) {
            throw new IllegalArgumentException("Jokabide desegokia ez dagokio ikasle honi.");
        }
        return jokabidea;
    }
}
