package com.koadernoa.app.objektuak.koadernoak.service;

import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Asistentzia;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Saioa;
import com.koadernoa.app.objektuak.koadernoak.repository.AsistentziaRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.SaioaRepository;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;
import com.koadernoa.app.objektuak.modulua.repository.MatrikulaRepository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FaltenExcelInportService {

    private static final String SHEET_NAME = "faltas";

    private final MatrikulaRepository matrikulaRepository;
    private final SaioaRepository saioaRepository;
    private final AsistentziaRepository asistentziaRepository;
    private final AsistentziaService asistentziaService;

    @Transactional
    public InportEmaitza inportatu(Koadernoa koadernoa, MultipartFile fitxategia) throws IOException {
        if (fitxategia == null || fitxategia.isEmpty()) {
            throw new IllegalArgumentException("Ez da fitxategirik jaso.");
        }

        String izena = fitxategia.getOriginalFilename() == null ? "" : fitxategia.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!izena.endsWith(".xls") && !izena.endsWith(".xlsx")) {
            throw new IllegalArgumentException(".xls edo .xlsx fitxategia behar da.");
        }

        List<Matrikula> matrikulak = matrikulaRepository.findByKoadernoaIdAndEgoeraMatrikulatuta(koadernoa.getId());
        Map<String, List<Matrikula>> matrikulaMap = eraikiMatrikulaMap(matrikulak);

        List<ImportRow> rows = new ArrayList<>();
        Set<LocalDate> datak = new HashSet<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook wb = WorkbookFactory.create(fitxategia.getInputStream())) {
            Sheet sh = wb.getSheet(SHEET_NAME);
            if (sh == null) {
                throw new IllegalArgumentException("Ez da aurkitu 'faltas' orria.");
            }

            Map<String, Integer> col = readHeaderFlexible(sh.getRow(0));
            for (int r = 1; r <= sh.getLastRowNum(); r++) {
                Row row = sh.getRow(r);
                if (row == null) continue;

                String alumnoRaw = getCellStr(row, col.get("ALUMNO"), formatter);
                LocalDate data = getCellDate(row.getCell(col.get("FECHA")), formatter);
                Integer slot = parseSlot(getCellStr(row, col.get("TIEMPO"), formatter));
                Boolean justifikatua = parseJustifikatua(getCellStr(row, col.get("JUSTIFICADA"), formatter));

                if (alumnoRaw == null || data == null || slot == null) {
                    continue;
                }

                rows.add(new ImportRow(r + 1, alumnoRaw, data, slot, Boolean.TRUE.equals(justifikatua)));
                datak.add(data);
            }
        }

        if (rows.isEmpty()) {
            return new InportEmaitza(0, 0, 0, List.of("Ez da lerro baliagarririk aurkitu."));
        }

        for (LocalDate data : datak) {
            asistentziaService.ensureSaioakForDate(koadernoa, data);
        }

        asistentziaRepository.deleteByKoadernoaIdAndSaioaDataIn(koadernoa.getId(), datak);

        Map<String, Saioa> saioMap = new HashMap<>();
        LocalDate minDate = datak.stream().min(LocalDate::compareTo).orElseThrow();
        LocalDate maxDate = datak.stream().max(LocalDate::compareTo).orElseThrow();
        saioaRepository.findByKoadernoaIdAndDataBetweenOrderByDataAscHasieraSlotAsc(koadernoa.getId(), minDate, maxDate)
                .forEach(s -> saioMap.put(key(s.getData(), s.getHasieraSlot()), s));

        int sortuak = 0;
        int baztertuak = 0;
        List<String> oharrak = new ArrayList<>();

        for (ImportRow row : rows) {
            Optional<Matrikula> matrikulaOpt = matchMatrikula(matrikulaMap, row.alumno());
            if (matrikulaOpt.isEmpty()) {
                baztertuak++;
                oharrak.add("R" + row.rowNum() + ": ikaslea ez da aurkitu -> " + row.alumno());
                continue;
            }

            Saioa saioa = saioMap.get(key(row.data(), row.slot()));
            if (saioa == null) {
                baztertuak++;
                oharrak.add("R" + row.rowNum() + ": saiorik ez data/ordu horretan -> " + row.data() + " / " + row.slot());
                continue;
            }

            asistentziaService.markatu(
                    saioa.getId(),
                    matrikulaOpt.get().getId(),
                    row.justifikatua() ? Asistentzia.AsistentziaEgoera.JUSTIFIKATUA : Asistentzia.AsistentziaEgoera.HUTS,
                    row.justifikatua() ? "Excel inportazioa" : null,
                    null
            );
            sortuak++;
        }

        return new InportEmaitza(rows.size(), sortuak, baztertuak, oharrak);
    }

    private Optional<Matrikula> matchMatrikula(Map<String, List<Matrikula>> matrikulaMap, String alumnoRaw) {
        String key = excelAlumnoToKey(alumnoRaw);
        if (key != null) {
            List<Matrikula> aukerak = matrikulaMap.get(key);
            if (aukerak != null && !aukerak.isEmpty()) {
                return Optional.of(aukerak.get(0));
            }
        }

        String fallback = normalizeName(alumnoRaw);
        return matrikulaMap.entrySet().stream()
                .filter(e -> normalizeName(e.getKey()).equals(fallback))
                .findFirst()
                .flatMap(e -> e.getValue().stream().findFirst());
    }

    private Map<String, List<Matrikula>> eraikiMatrikulaMap(List<Matrikula> matrikulak) {
        Map<String, List<Matrikula>> out = new HashMap<>();
        for (Matrikula m : matrikulak) {
            String key = studentToKey(m);
            if (key == null) continue;
            out.computeIfAbsent(key, __ -> new ArrayList<>()).add(m);
        }
        return out;
    }

    private String studentToKey(Matrikula m) {
        if (m.getIkaslea() == null) return null;
        String ab = normalizeName(joinParts(m.getIkaslea().getAbizena1(), m.getIkaslea().getAbizena2()));
        String iz = normalizeName(m.getIkaslea().getIzena());
        if (ab == null || iz == null) return null;
        return ab + "," + iz;
    }

    private String excelAlumnoToKey(String alumno) {
        if (alumno == null) return null;
        String[] parts = alumno.split(",", 2);
        if (parts.length == 2) {
            String ab = normalizeName(parts[0]);
            String iz = normalizeName(parts[1]);
            if (ab != null && iz != null) {
                return ab + "," + iz;
            }
        }
        return null;
    }

    private String joinParts(String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(value.trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private String normalizeName(String s) {
        if (s == null || s.isBlank()) return null;
        String t = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{Alnum}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
        return t.isBlank() ? null : t;
    }

    private String key(LocalDate data, int slot) {
        return data + "#" + slot;
    }

    private Map<String, Integer> readHeaderFlexible(Row header) {
        Map<String, Integer> byName = new HashMap<>();
        if (header != null) {
            short last = header.getLastCellNum();
            for (int i = 0; i < last; i++) {
                String key = normalizeHeader(getCellStr(header, i, new DataFormatter()));
                if (key != null) byName.put(key, i);
            }
        }

        Map<String, Integer> out = new HashMap<>();
        out.put("ALUMNO", findCol(byName, "ALUMNO"));
        out.put("FECHA", findCol(byName, "FECHA"));
        out.put("TIEMPO", findCol(byName, "TIEMPO"));
        out.put("JUSTIFICADA", findCol(byName, "JUSTIFICADA"));

        if (out.values().stream().anyMatch(v -> v == null)) {
            throw new IllegalArgumentException("Goiburuak ez dira zuzenak. Beharrezkoak: ALUMNO, FECHA, TIEMPO, JUSTIFICADA");
        }
        return out;
    }

    private Integer findCol(Map<String, Integer> byName, String expected) {
        String normExpected = normalizeHeader(expected);
        Integer exact = byName.get(normExpected);
        if (exact != null) return exact;
        return byName.entrySet().stream()
                .filter(e -> e.getKey().contains(normExpected))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String normalizeHeader(String s) {
        if (s == null) return null;
        return normalizeName(s.toUpperCase(Locale.ROOT).replace('_', ' '));
    }

    private String getCellStr(Row row, Integer idx, DataFormatter formatter) {
        if (row == null || idx == null || idx < 0) return null;
        return getCellStr(row, idx.intValue(), formatter);
    }

    private String getCellStr(Row row, int idx, DataFormatter formatter) {
        Cell c = row.getCell(idx);
        if (c == null) return null;
        String v = formatter.formatCellValue(c);
        if (v == null) return null;
        String t = v.trim();
        return t.isBlank() ? null : t;
    }

    private LocalDate getCellDate(Cell c, DataFormatter formatter) {
        if (c == null) return null;

        if (c.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(c)) {
            return c.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }

        if (c.getCellType() == CellType.NUMERIC) {
            try {
                return DateUtil.getLocalDateTime(c.getNumericCellValue()).toLocalDate();
            } catch (Exception ignored) {
                // fallback string-era
            }
        }

        String txt = formatter.formatCellValue(c);
        if (txt == null || txt.isBlank()) return null;

        List<DateTimeFormatter> formatuak = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ISO_LOCAL_DATE
        );
        for (DateTimeFormatter f : formatuak) {
            try {
                return LocalDate.parse(txt.trim(), f);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Integer parseSlot(String tiempoRaw) {
        if (tiempoRaw == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,2})").matcher(tiempoRaw);
        if (!m.find()) return null;
        int slot = Integer.parseInt(m.group(1));
        return (slot >= 1 && slot <= 12) ? slot : null;
    }

    private Boolean parseJustifikatua(String raw) {
        if (raw == null) return Boolean.FALSE;
        String n = normalizeName(raw);
        if (n == null) return Boolean.FALSE;
        return n.equals("bai") || n.equals("si") || n.equals("yes") || n.equals("justificada") || n.equals("1") || n.equals("true");
    }

    @Getter
    @AllArgsConstructor
    public static class InportEmaitza {
        private int irakurritakoak;
        private int sortuak;
        private int baztertuak;
        private List<String> oharrak;
    }

    private record ImportRow(int rowNum, String alumno, LocalDate data, int slot, boolean justifikatua) {
    }
}
