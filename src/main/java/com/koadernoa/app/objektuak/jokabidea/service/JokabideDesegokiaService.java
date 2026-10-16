package com.koadernoa.app.objektuak.jokabidea.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import com.koadernoa.app.objektuak.egutegia.entitateak.Ikasturtea;
import com.koadernoa.app.objektuak.egutegia.repository.IkasturteaRepository;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.objektuak.irakasleak.entitateak.Rola;
import com.koadernoa.app.objektuak.irakasleak.repository.IrakasleaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import com.koadernoa.app.objektuak.jokabidea.repository.JokabideDesegokiaRepository;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.mezuak.service.MezuaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JokabideDesegokiaService {
    private static final Logger log = LoggerFactory.getLogger(JokabideDesegokiaService.class);

    private final JokabideDesegokiaRepository repository;
    private final AplikazioAukeraService aplikazioAukeraService;
    private final IrakasleaRepository irakasleaRepository;
    private final MezuaService mezuaService;
    private final IkasturteaRepository ikasturteaRepository;

    @Transactional(rollbackFor = Exception.class)
    public JokabideDesegokia sortu(JokabideDesegokia jokabidea) {
        JokabideDesegokia gordeta = repository.saveAndFlush(jokabidea);
        Optional<Irakaslea> arduraduna = lortuArduraduna();
        if (arduraduna.isEmpty()) {
            log.warn("Jokabide desegokia sortu da, baina ez dago jokabide desegokien arduradunik konfiguratuta (id={}).",
                    gordeta.getId());
            return gordeta;
        }
        Optional<Ikasturtea> ikasturtea = ikasturteaRepository.findFirstByAktiboaTrueOrderByIdDesc();
        long kopurua = ikasturtea
                .map(i -> repository.countByIkasleaIdAndIkasturteaId(gordeta.getIkaslea().getId(), i.getId()))
                .orElse(0L);
        String esteka = sortuIkaslearenEsteka(gordeta, ikasturtea.orElse(null));
        mezuaService.bidaliJokabideDesegokiarenAbisua(gordeta, arduraduna.get(), kopurua, esteka);
        return gordeta;
    }

    private String sortuIkaslearenEsteka(JokabideDesegokia jokabidea, Ikasturtea ikasturtea) {
        String ikasleFiltroa = String.join(" ", java.util.stream.Stream.of(
                        jokabidea.getIkaslea().getAbizena1(),
                        jokabidea.getIkaslea().getAbizena2(),
                        jokabidea.getIkaslea().getIzena())
                .filter(zatia -> zatia != null && !zatia.isBlank())
                .map(String::trim)
                .toList());
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromPath("/kudeatzaile/jokabide-desegokiak")
                .queryParam("ikasleaId", jokabidea.getIkaslea().getId())
                .queryParam("ikaslea", ikasleFiltroa);
        if (ikasturtea != null && ikasturtea.getIzena() != null) {
            try {
                String[] urteak = ikasturtea.getIzena().trim().split("-");
                builder.queryParam("dataHasiera", urteak[0] + "-09-01")
                        .queryParam("dataAmaiera", urteak[1] + "-08-31");
            } catch (RuntimeException ex) {
                log.warn("Ezin izan dira ikasturte aktiboaren datak estekan prestatu: {}", ikasturtea.getIzena());
            }
        }
        return builder.build().encode().toUriString();
    }

    private Optional<Irakaslea> lortuArduraduna() {
        Long arduradunaId = aplikazioAukeraService.getJokabideDesegokienArduradunaId();
        if (arduradunaId == null) return Optional.empty();
        return irakasleaRepository.findById(arduradunaId)
                .filter(irakaslea -> irakaslea.getRola() == Rola.KUDEATZAILEA);
    }
}
