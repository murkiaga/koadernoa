package com.koadernoa.app.funtzionalitateak.irakasle.tutorea;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.koadernoa.app.funtzionalitateak.irakasle.baimenak.ikasleakkontsultatu.IkasleakKontsultatuService;
import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.service.IrakasleaService;
import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.security.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TutoretzaService {

    private final IkasleakKontsultatuService ikasleakKontsultatuService;
    private final IrakasleaService irakasleaService;
    private final IkasturteaRepository ikasturteaRepository;
    private final MatrikulaRepository matrikulaRepository;

    public Taldea baimenduTaldea(Long taldeaId, Authentication authentication) {
        Taldea taldea = ikasleakKontsultatuService.getTaldea(taldeaId);
        if (SecurityUtils.isKudeatzailea(authentication)) {
            return taldea;
        }
        if (!SecurityUtils.hasAnyRole(authentication, "IRAKASLEA")) {
            throw new AccessDeniedException("Ez daukazu tutoretza atalera sartzeko baimenik.");
        }
        Long irakasleaId;
        try {
            irakasleaId = irakasleaService.getLogeatutaDagoenIrakaslea(authentication).getId();
        } catch (RuntimeException ex) {
            throw new AccessDeniedException("Ezin izan da irakaslea identifikatu.", ex);
        }
        if (taldea.getTutorea() == null || !irakasleaId.equals(taldea.getTutorea().getId())) {
            throw new AccessDeniedException("Irakaslea ez da talde honetako tutorea.");
        }
        return taldea;
    }

    public Ikasturtea getIkasturteAktiboa() {
        return ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT, "Ez dago ikasturte aktiborik."));
    }

    public List<Ikaslea> getTaldekoIkasleAktiboak(Long taldeaId) {
        Ikasturtea ikasturtea = getIkasturteAktiboa();
        return matrikulaRepository.findTaldekoIkasleakByIkasturtea(taldeaId, ikasturtea.getId());
    }

    public Ikaslea baimenduIkaslea(Long taldeaId, Long ikasleaId) {
        Ikasturtea ikasturtea = getIkasturteAktiboa();
        if (!matrikulaRepository.existsTaldekoMatrikulaByIkasturtea(
                taldeaId, ikasleaId, ikasturtea.getId())) {
            throw new AccessDeniedException(
                    "Ikaslea ez da talde honetakoa edo ez dauka ikasturte aktiboko matrikularik.");
        }
        return ikasleakKontsultatuService.getIkaslea(ikasleaId);
    }
}
