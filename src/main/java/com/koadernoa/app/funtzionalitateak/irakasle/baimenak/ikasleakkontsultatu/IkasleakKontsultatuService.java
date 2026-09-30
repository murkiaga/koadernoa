package com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
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

    public List<JokabideDesegokia> getJokabideDesegokiak(Long ikasleaId) {
        getIkaslea(ikasleaId);
        return jokabideDesegokiaRepository.findAllByIkasleaIdForKontsulta(ikasleaId);
    }

    public boolean badituJokabideDesegokiak(Long ikasleaId) {
        return jokabideDesegokiaRepository.existsByIkasleaId(ikasleaId);
    }
}
