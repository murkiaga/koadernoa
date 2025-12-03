package com.koadernoa.app.objektuak.ebaluazioa.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.ebaluazioa.repository.EbaluazioNotaRepository;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EbaluazioNotaService {

	private final EbaluazioNotaRepository ebaluazioNotaRepository;

    /**
     * Notak eta egoerak gorde / eguneratu.
     * @return error mezua (HTML) baldin badago; bestela null edo string hutsa.
     */
    @Transactional
    public String gordeNotak(Koadernoa koadernoa,
                             List<EbaluazioMomentua> momentuak,
                             List<Matrikula> matrikulak,
                             HttpServletRequest request) {

        StringBuilder errorBuilder = new StringBuilder();

        for (Matrikula matrikula : matrikulak) {
            String ikasleIzena = matrikula.getIkaslea() != null
                    ? matrikula.getIkaslea().getIzenOsoa()
                    : ("Matrikula ID " + matrikula.getId());

            for (EbaluazioMomentua momentua : momentuak) {
                String paramName = "nota_" + matrikula.getId() + "_" + momentua.getId();
                String rawValue = request.getParameter(paramName);

                if (rawValue == null) continue; // ez dago inputik (ez du aldatu)

                String value = rawValue.trim();
                if (value.isEmpty()) {
                    // Hutsa → nota ezabatu (egoera eta nota)
                    ebaluazioNotaRepository
                            .findByMatrikulaAndEbaluazioMomentua(matrikula, momentua)
                            .ifPresent(ebaluazioNotaRepository::delete);
                    continue;
                }

                // ===== 1) Saiatu egoera berezi bat dela (kodea) =====
                final String kodeValue = value; // lambda-rako "effectively final"
                var egoeraOpt = momentua.getEgoeraOnartuak().stream()
                        .filter(e -> e.getKodea().equalsIgnoreCase(kodeValue))
                        .findFirst();

                var existingOpt = ebaluazioNotaRepository
                        .findByMatrikulaAndEbaluazioMomentua(matrikula, momentua);

                if (egoeraOpt.isPresent()) {
                    EbaluazioNota nota = existingOpt.orElseGet(() -> {
                        EbaluazioNota berria = new EbaluazioNota();
                        berria.setMatrikula(matrikula);
                        berria.setEbaluazioMomentua(momentua);
                        return berria;
                    });

                    nota.setEgoera(egoeraOpt.get());
                    nota.setNota(null); // egoera berezi gehienek ez dute nota zenbakizkorik
                    ebaluazioNotaRepository.save(nota);
                    continue;
                }

                // ===== 2) Bestela, saiatu nota zenbakizkoa dela =====
                String num = value.replace(',', '.');
                Double notaZenbaki = null;
                boolean numeric = true;
                try {
                    notaZenbaki = Double.valueOf(num);
                } catch (NumberFormatException ex) {
                    numeric = false;
                }

                if (numeric) {
                    // Momentu honek ez badu nota zenbakizkorik onartzen → ERROREA
                    if (!Boolean.TRUE.equals(momentua.getOnartuNotaZenbakizkoa())) {
                        errorBuilder.append("Ikaslea ")
                                .append(ikasleIzena)
                                .append(", \"")
                                .append(momentua.getIzena())
                                .append("\" momentuan: nota zenbakizkorra ez dago baimenduta (")
                                .append(value)
                                .append(").").append('\n');
                        continue;
                    }

                    // 0–10 tartea
                    if (notaZenbaki < 0 || notaZenbaki > 10) {
                        errorBuilder.append("Ikaslea ")
                                .append(ikasleIzena)
                                .append(", \"")
                                .append(momentua.getIzena())
                                .append("\" momentuan: nota ")
                                .append(value)
                                .append(" ez da baliozkoa (0–10 artekoa izan behar du).")
                                .append('\n');
                        continue;
                    }

                    EbaluazioNota nota = existingOpt.orElseGet(() -> {
                        EbaluazioNota berria = new EbaluazioNota();
                        berria.setMatrikula(matrikula);
                        berria.setEbaluazioMomentua(momentua);
                        return berria;
                    });

                    nota.setNota(notaZenbaki);
                    nota.setEgoera(null); // egoera berezirik ez
                    ebaluazioNotaRepository.save(nota);
                } else {
                    // ===== 3) Ez da egoera baliozkoa, ezta zenbaki egokia ere =====
                    errorBuilder.append("Ikaslea ")
                            .append(ikasleIzena)
                            .append(", \"")
                            .append(momentua.getIzena())
                            .append("\" momentuan: \"")
                            .append(value)
                            .append("\" balioa ez da ez egoera baimendua, ez nota zenbakizko egokia.")
                            .append('\n');
                }
            }
        }

        if (errorBuilder.length() == 0) {
            return null;
        }
        // Lerro-jauziak HTML <br>-ekin
        return errorBuilder.toString();
    }
}
