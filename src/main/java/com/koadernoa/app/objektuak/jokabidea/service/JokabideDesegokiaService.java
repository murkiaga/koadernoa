package com.koadernoa.app.objektuak.jokabidea.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = Exception.class)
    public JokabideDesegokia sortu(JokabideDesegokia jokabidea) {
        JokabideDesegokia gordeta = repository.saveAndFlush(jokabidea);
        Optional<Irakaslea> arduraduna = lortuArduraduna();
        if (arduraduna.isEmpty()) {
            log.warn("Jokabide desegokia sortu da, baina ez dago jokabide desegokien arduradunik konfiguratuta (id={}).",
                    gordeta.getId());
            return gordeta;
        }
        mezuaService.bidaliJokabideDesegokiarenAbisua(gordeta, arduraduna.get());
        return gordeta;
    }

    private Optional<Irakaslea> lortuArduraduna() {
        Long arduradunaId = aplikazioAukeraService.getJokabideDesegokienArduradunaId();
        if (arduradunaId == null) return Optional.empty();
        return irakasleaRepository.findById(arduradunaId)
                .filter(irakaslea -> irakaslea.getRola() == Rola.KUDEATZAILEA);
    }
}
