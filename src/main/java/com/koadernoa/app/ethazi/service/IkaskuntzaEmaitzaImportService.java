package com.koadernoa.app.ethazi.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow;
import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow.Egoera;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;
import com.koadernoa.app.objektuak.modulua.repository.IkaskuntzaEmaitzaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IkaskuntzaEmaitzaImportService {
    private final IkaskuntzaEmaitzaRepository repository;

    public record Laburpena(int sortuak, int eguneratuak, int aldaketarikGabe, int arazoak) {}

    @Transactional(readOnly = true)
    public List<IkaskuntzaEmaitzaImportRow> aurrebista(List<IkaskuntzaEmaitzaImportRow> parsed) {
        Map<String, Integer> occurrences = new HashMap<>();
        parsed.stream().filter(this::hasKey).forEach(row -> occurrences.merge(key(row), 1, Integer::sum));
        List<IkaskuntzaEmaitzaImportRow> result = new ArrayList<>();
        for (var row : parsed) {
            String problem = validate(row);
            if (problem == null && occurrences.getOrDefault(key(row), 0) > 1)
                problem = "PDFan modulu-kode eta ordena bera behin baino gehiagotan agertu da.";
            if (problem != null) {
                result.add(row.preview(Egoera.ARAZOA, problem, null));
                continue;
            }
            var existing = repository.findByEeiKodeaAndOrdena(row.eeiKodea(), row.ordena());
            String current = existing.map(value -> description(value, row.hizkuntza())).orElse(null);
            Egoera status = existing.isEmpty() ? Egoera.SORTU
                    : Objects.equals(normalize(current), normalize(row.deskribapena()))
                            ? Egoera.ALDAKETARIK_EZ : Egoera.EGUNERATU;
            result.add(row.preview(status, null, current));
        }
        return List.copyOf(result);
    }

    @Transactional
    public Laburpena inportatu(List<IkaskuntzaEmaitzaImportRow> preview) {
        List<IkaskuntzaEmaitzaImportRow> current = aurrebista(preview);
        int created = 0, updated = 0, unchanged = 0, problems = 0;
        for (var row : current) {
            if (row.egoera() == Egoera.ARAZOA) { problems++; continue; }
            if (row.egoera() == Egoera.ALDAKETARIK_EZ) { unchanged++; continue; }
            var found = repository.findByEeiKodeaAndOrdena(row.eeiKodea(), row.ordena());
            IkaskuntzaEmaitza entity = found.orElseGet(IkaskuntzaEmaitza::new);
            if (found.isEmpty()) {
                entity.setEeiKodea(row.eeiKodea());
                entity.setOrdena(row.ordena());
                entity.setKodea(row.kodea());
                // deskribapena NOT NULL da egungo eskeman; ES-only sorrerak ez du EU testurik asmatzen.
                entity.setDeskribapena("");
            }
            if (row.hizkuntza() == Hizkuntza.EUSKARA) entity.setDeskribapena(row.deskribapena());
            else entity.setDeskribapenaEs(row.deskribapena());
            repository.save(entity);
            if (found.isEmpty()) created++; else updated++;
        }
        repository.flush();
        return new Laburpena(created, updated, unchanged, problems);
    }

    public byte[] csv(List<IkaskuntzaEmaitzaImportRow> rows) {
        StringBuilder csv = new StringBuilder("\uFEFFeeiKodea,ordena,kodea,hizkuntza,deskribapena,dbBalioa,egoera,mezua\r\n");
        for (var row : rows) {
            csv.append(escape(row.eeiKodea())).append(',').append(row.ordena() == null ? "" : row.ordena()).append(',')
                    .append(escape(row.kodea())).append(',').append(row.hizkuntza()).append(',')
                    .append(escape(row.deskribapena())).append(',').append(escape(row.dbBalioa())).append(',')
                    .append(row.egoera()).append(',').append(escape(row.mezua())).append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String validate(IkaskuntzaEmaitzaImportRow row) {
        if (!hasKey(row)) return "Modulu-kodea eta ordena behar dira.";
        if (!row.eeiKodea().matches("(?:\\d{4}|[A-Za-z]\\d{3})")) return "Modulu-kodearen formatua ez da ezagutzen.";
        if (row.ordena() <= 0) return "Ordenak zero baino handiagoa izan behar du.";
        if (row.deskribapena() == null || row.deskribapena().isBlank()) return "Deskribapena hutsik dago.";
        if (row.deskribapena().length() > 60000) return "Deskribapena luzeegia da.";
        if (row.hizkuntza() != Hizkuntza.EUSKARA && row.hizkuntza() != Hizkuntza.GAZTELERA) return "Hizkuntza ez da onartzen.";
        return null;
    }

    private boolean hasKey(IkaskuntzaEmaitzaImportRow row) { return row.eeiKodea() != null && row.ordena() != null; }
    private String key(IkaskuntzaEmaitzaImportRow row) { return row.eeiKodea() + "\u0000" + row.ordena(); }
    private String description(IkaskuntzaEmaitza entity, Hizkuntza language) {
        return language == Hizkuntza.EUSKARA ? entity.getDeskribapena() : entity.getDeskribapenaEs();
    }
    private String normalize(String value) { return value == null ? "" : value.strip().replaceAll("\\s+", " "); }
    private String escape(Object value) {
        if (value == null) return "";
        return "\"" + value.toString().replace("\"", "\"\"") + "\"";
    }
}
