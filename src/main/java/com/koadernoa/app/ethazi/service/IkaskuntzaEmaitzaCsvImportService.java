package com.koadernoa.app.ethazi.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.modulua.entitateak.IkaskuntzaEmaitza;
import com.koadernoa.app.objektuak.modulua.repository.IkaskuntzaEmaitzaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IkaskuntzaEmaitzaCsvImportService {
    private static final List<String> REQUIRED_HEADERS = List.of(
            "eeiKodea", "ordena", "kodea", "deskribapenaEU", "deskribapenaES");

    private final IkaskuntzaEmaitzaRepository emaitzak;

    public record InportazioEmaitza(int sortuak, int eguneratuak) {
        public int guztira() { return sortuak + eguneratuak; }
    }

    private record CsvEmaitza(String eeiKodea, int ordena, String kodea,
            String deskribapenaEu, String deskribapenaEs) {}

    @Transactional
    public InportazioEmaitza inportatu(MultipartFile fitxategia) {
        if (fitxategia == null || fitxategia.isEmpty()) {
            throw new IllegalArgumentException("Aukeratu CSV fitxategi bat.");
        }

        List<CsvEmaitza> lerroak;
        try (var reader = new InputStreamReader(fitxategia.getInputStream(), StandardCharsets.UTF_8)) {
            lerroak = irakurri(reader);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Ezin izan da CSV fitxategia irakurri.", ex);
        }

        int sortuak = 0;
        int eguneratuak = 0;
        for (CsvEmaitza lerroa : lerroak) {
            var aurkitua = emaitzak.findByEeiKodeaAndOrdena(lerroa.eeiKodea(), lerroa.ordena());
            var emaitza = aurkitua.orElseGet(IkaskuntzaEmaitza::new);
            emaitza.setEeiKodea(lerroa.eeiKodea());
            emaitza.setOrdena(lerroa.ordena());
            emaitza.setKodea(lerroa.kodea());
            emaitza.setDeskribapena(lerroa.deskribapenaEu());
            emaitza.setDeskribapenaEs(lerroa.deskribapenaEs());
            // CSVak ez du ingelesezko zutaberik: lehendik eskuz sartutakoa ez da ezabatzen.
            emaitzak.save(emaitza);
            if (aurkitua.isPresent()) eguneratuak++; else sortuak++;
        }
        emaitzak.flush();
        return new InportazioEmaitza(sortuak, eguneratuak);
    }

    private List<CsvEmaitza> irakurri(java.io.Reader input) throws IOException {
        List<List<String>> records = csvRecords(input);
        if (records.isEmpty()) throw new IllegalArgumentException("CSV fitxategia hutsik dago.");

        List<String> headers = records.get(0);
        if (!headers.isEmpty() && headers.get(0).startsWith("\uFEFF")) {
            headers.set(0, headers.get(0).substring(1));
        }
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i).strip();
            if (columns.put(header, i) != null) {
                throw new IllegalArgumentException("CSV goiburuan zutabe bikoiztua dago: " + header);
            }
        }
        for (String required : REQUIRED_HEADERS) {
            if (!columns.containsKey(required)) {
                throw new IllegalArgumentException("CSV goiburuan zutabe hau falta da: " + required);
            }
        }

        List<CsvEmaitza> result = new ArrayList<>();
        Set<String> keys = new HashSet<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> row = records.get(i);
            if (row.stream().allMatch(String::isBlank)) continue;
            int line = i + 1;
            if (row.size() != headers.size()) {
                throw new IllegalArgumentException(line + ". lerroak " + row.size()
                        + " eremu ditu; " + headers.size() + " espero ziren.");
            }
            String eeiKodea = required(row, columns, "eeiKodea", line, 255);
            String kodea = required(row, columns, "kodea", line, 20);
            String eu = required(row, columns, "deskribapenaEU", line, 60000);
            String es = optional(row, columns, "deskribapenaES", 60000, line);
            int ordena;
            try {
                ordena = Integer.parseInt(required(row, columns, "ordena", line, 10));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(line + ". lerroko ordena ez da zenbaki oso bat.");
            }
            if (ordena <= 0) throw new IllegalArgumentException(line + ". lerroko ordenak zero baino handiagoa izan behar du.");
            if (!keys.add(eeiKodea + "\u0000" + ordena)) {
                throw new IllegalArgumentException(line + ". lerroan EEI kode eta ordena bera errepikatuta dago.");
            }
            result.add(new CsvEmaitza(eeiKodea, ordena, kodea, eu, es));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("CSV fitxategiak ez du datu-lerrorik.");
        return result;
    }

    private String required(List<String> row, Map<String, Integer> columns, String name, int line, int max) {
        String value = row.get(columns.get(name)).strip();
        if (value.isEmpty()) throw new IllegalArgumentException(line + ". lerroko " + name + " bete behar da.");
        if (value.length() > max) throw new IllegalArgumentException(line + ". lerroko " + name + " luzeegia da.");
        return value;
    }

    private String optional(List<String> row, Map<String, Integer> columns, String name, int max, int line) {
        String value = row.get(columns.get(name)).strip();
        if (value.length() > max) throw new IllegalArgumentException(line + ". lerroko " + name + " luzeegia da.");
        return value.isEmpty() ? null : value;
    }

    /** RFC 4180ko komatxoak, komak eta lerro-jauziak onartzen dituen irakurle txikia. */
    private List<List<String>> csvRecords(java.io.Reader input) throws IOException {
        var reader = new PushbackReader(new BufferedReader(input), 1);
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean fieldStarted = false;
        boolean firstCharacter = true;
        int value;
        while ((value = reader.read()) != -1) {
            char c = (char) value;
            if (firstCharacter && c == '\uFEFF') {
                firstCharacter = false;
                continue;
            }
            firstCharacter = false;
            if (quoted) {
                if (c == '"') {
                    int next = reader.read();
                    if (next == '"') field.append('"');
                    else {
                        quoted = false;
                        if (next != -1) reader.unread(next);
                    }
                } else field.append(c);
            } else if (c == '"' && !fieldStarted) {
                quoted = true;
                fieldStarted = true;
            } else if (c == ',') {
                row.add(field.toString()); field.setLength(0); fieldStarted = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r') {
                    int next = reader.read();
                    if (next != '\n' && next != -1) reader.unread(next);
                }
                row.add(field.toString()); records.add(row);
                row = new ArrayList<>(); field.setLength(0); fieldStarted = false;
            } else {
                field.append(c); fieldStarted = true;
            }
        }
        if (quoted) throw new IllegalArgumentException("CSV fitxategian itxi gabeko komatxo bat dago.");
        if (field.length() > 0 || fieldStarted || !row.isEmpty()) {
            row.add(field.toString()); records.add(row);
        }
        return records;
    }
}
