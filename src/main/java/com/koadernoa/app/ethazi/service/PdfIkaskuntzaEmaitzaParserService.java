package com.koadernoa.app.ethazi.service;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.ethazi.dto.IkaskuntzaEmaitzaImportRow;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;

@Service
public class PdfIkaskuntzaEmaitzaParserService {
    private static final Pattern MODULE_DOTTED = Pattern.compile(
            "^\\s*((?:\\d{4}|[A-Za-z]\\d{3}))\\s*[.·:\\-–—]\\s+.+$");
    private static final Pattern MODULE_CODE = Pattern.compile(
            "(?iu)^\\s*(?:kodea|c[oó]digo)\\s*[:.-]\\s*((?:\\d{4}|[A-Za-z]\\d{3}))\\b.*$");
    private static final Pattern OUTCOME = Pattern.compile(
            "^\\s*(\\d{1,2})\\s*(?:[.)]\\s*[-–—]*|[-–—]+)\\s*(.+?)\\s*$");
    private static final Pattern OUTCOME_NUMBER_ONLY = Pattern.compile(
            "^\\s*(\\d{1,2})\\s*(?:[.)]\\s*[-–—]*|[-–—]+)\\s*$");
    private static final Pattern CRITERION = Pattern.compile("(?iu)^\\s*[a-zñ]\\s*[.)]\\s+.*$");
    private static final Pattern INLINE_CRITERION = Pattern.compile("(?iu)\\s+[a-zñ]\\s*[.)]\\s+");

    public record ParserEmaitza(List<IkaskuntzaEmaitzaImportRow> lerroak, List<String> abisuak) {}

    public ParserEmaitza parse(MultipartFile file, Hizkuntza language) {
        validate(file, language);
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("PDFa pasahitzez babestuta dago eta ezin da irakurri.");
            }
            return parseText(new PDFTextStripper().getText(document), language);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Ezin izan da PDF fitxategia irakurri.", ex);
        }
    }

    private void validate(MultipartFile file, Hizkuntza language) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Aukeratu PDF fitxategi bat.");
        if (language != Hizkuntza.EUSKARA && language != Hizkuntza.GAZTELERA)
            throw new IllegalArgumentException("Aukeratu Euskara edo Gaztelera.");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".pdf"))
            throw new IllegalArgumentException("Fitxategiak PDF formatua izan behar du.");
    }

    /** Testu bidezko sarrera publikoa da, parserra PDF bitarretik aparte probatu ahal izateko. */
    public ParserEmaitza parseText(String text, Hizkuntza language) {
        if (language != Hizkuntza.EUSKARA && language != Hizkuntza.GAZTELERA)
            throw new IllegalArgumentException("Parserrak Euskara edo Gaztelera behar du.");
        List<IkaskuntzaEmaitzaImportRow> rows = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String moduleCode = null;
        boolean inOutcomes = false;
        boolean awaitingFirstOutcome = false;
        boolean inCriteria = false;
        Integer order = null;
        StringBuilder description = new StringBuilder();

        for (String raw : text.replace('\u00a0', ' ').split("\\R")) {
            String line = raw.strip().replaceAll("\\s+", " ");
            if (line.isBlank()) continue;

            Matcher explicitCode = MODULE_CODE.matcher(line);
            Matcher dottedCode = MODULE_DOTTED.matcher(line);
            boolean explicitCodeFound = explicitCode.matches();
            boolean dottedCodeFound = dottedCode.matches();
            if (explicitCodeFound || dottedCodeFound) {
                String detectedCode = (explicitCodeFound ? explicitCode.group(1) : dottedCode.group(1))
                        .toUpperCase(Locale.ROOT);
                // Orri bakoitzeko goiburuak modulu-kode bera errepika dezake. Ez dugu uneko
                // emaitza mozten: deskribapenak hurrengo orrian jarrai dezake.
                if (detectedCode.equals(moduleCode)) continue;
                flush(rows, moduleCode, order, description, language);
                if (moduleCode != null && !moduleCode.equals(detectedCode) && !awaitingFirstOutcome)
                    inOutcomes = false;
                moduleCode = detectedCode;
                order = null;
                description.setLength(0);
                inCriteria = false;
                continue;
            }

            String normalized = normalize(line);
            if (isModuleMetadata(normalized)) continue;
            if (isOutcomeSection(normalized, language)) {
                flush(rows, moduleCode, order, description, language);
                order = null;
                description.setLength(0);
                inOutcomes = true;
                awaitingFirstOutcome = true;
                inCriteria = false;
                continue;
            }
            if (inOutcomes && isNextSection(normalized, language)) {
                flush(rows, moduleCode, order, description, language);
                order = null;
                description.setLength(0);
                inOutcomes = false;
                awaitingFirstOutcome = false;
                inCriteria = false;
                continue;
            }
            if (!inOutcomes) continue;

            // DCBek emaitza bakoitzaren ondoren etiketa hau jartzen dute. Etiketa bera eta
            // hurrengo irizpideak ez dira IEaren deskribapenaren parte.
            if (isCriteriaHeading(normalized, language)) {
                inCriteria = true;
                continue;
            }

            Matcher outcomeNumberOnly = OUTCOME_NUMBER_ONLY.matcher(line);
            if (outcomeNumberOnly.matches()) {
                flush(rows, moduleCode, order, description, language);
                order = Integer.valueOf(outcomeNumberOnly.group(1));
                awaitingFirstOutcome = false;
                description.setLength(0);
                inCriteria = false;
                continue;
            }
            Matcher outcome = OUTCOME.matcher(line);
            if (outcome.matches()) {
                flush(rows, moduleCode, order, description, language);
                order = Integer.valueOf(outcome.group(1));
                awaitingFirstOutcome = false;
                description.setLength(0);
                appendBeforeCriterion(description, outcome.group(2));
                inCriteria = INLINE_CRITERION.matcher(outcome.group(2)).find();
                continue;
            }
            if (CRITERION.matcher(line).matches()) {
                inCriteria = true;
                continue;
            }
            if (order != null && !inCriteria && !isNoise(line)) {
                appendBeforeCriterion(description, line);
                if (INLINE_CRITERION.matcher(line).find()) inCriteria = true;
            }
        }
        flush(rows, moduleCode, order, description, language);

        if (rows.isEmpty()) warnings.add("Ez da ikaskuntza-emaitzarik detektatu. Egiaztatu DCB/curriculum PDFa eta aukeratutako hizkuntza.");
        else if (rows.size() < 3) warnings.add("Parserrak ikaskuntza-emaitza gutxi detektatu ditu; berrikusi aurrebista inportatu aurretik.");
        long modules = rows.stream().map(IkaskuntzaEmaitzaImportRow::eeiKodea).distinct().count();
        if (!rows.isEmpty() && modules == 1) warnings.add("Modulu bakarra detektatu da PDFan; egiaztatu hori dela espero zenuena.");
        return new ParserEmaitza(List.copyOf(rows), List.copyOf(warnings));
    }

    private void flush(List<IkaskuntzaEmaitzaImportRow> rows, String moduleCode, Integer order,
            StringBuilder description, Hizkuntza language) {
        if (order == null) return;
        String value = description.toString().strip();
        // Ertzeko testu bertikala PDFBoxek hitza zatituta eman dezake
        // (adib. "lanbide-m odulua"). Ez da inoiz aurrebista-lerro bat.
        if (isSpuriousModuleLabel(value)) return;
        if (moduleCode == null || moduleCode.isBlank()) {
            rows.add(new IkaskuntzaEmaitzaImportRow(null, order, "IE" + order, value, language,
                    IkaskuntzaEmaitzaImportRow.Egoera.ARAZOA,
                    "Ezin izan da ikaskuntza-emaitzaren modulu-kodea detektatu.", null));
        } else if (!value.isBlank()) {
            rows.add(new IkaskuntzaEmaitzaImportRow(moduleCode, order, "IE" + order, value,
                    language, null, null, null));
        }
    }

    private void appendBeforeCriterion(StringBuilder result, String line) {
        Matcher marker = INLINE_CRITERION.matcher(line);
        String value = marker.find() ? line.substring(0, marker.start()).strip() : line.strip();
        if (!value.isBlank()) {
            if (!result.isEmpty()) result.append(' ');
            result.append(value);
        }
    }

    private boolean isOutcomeSection(String line, Hizkuntza language) {
        return language == Hizkuntza.EUSKARA
                ? (line.contains("ikaskuntz") && line.contains("emaitz")
                        && line.contains("ebaluazio") && line.contains("irizp"))
                : (line.contains("resultados de aprendizaje") && line.contains("criterios de evaluacion"));
    }

    private boolean isNextSection(String line, Hizkuntza language) {
        return language == Hizkuntza.EUSKARA
                ? line.matches("^(?:(?:c|\\d+)\\s*[.)-]?\\s*)?(?:oinarrizko\\s+)?edukiak?(?:\\s*[:.]?.*)?$")
                        || line.matches("^(?:d\\s*[.)-]?\\s*)?orientabide\\s+metodologikoak?(?:\\s*[:.]?.*)?$")
                : line.matches("^(?:(?:c|\\d+)\\s*[.)-]?\\s*)?contenidos?(?:\\s+basicos)?(?:\\s*[:.]?.*)?$")
                        || line.matches("^(?:d\\s*[.)-]?\\s*)?orientaciones?\\s+metodologicas?(?:\\s*[:.]?.*)?$");
    }

    private boolean isCriteriaHeading(String line, Hizkuntza language) {
        return language == Hizkuntza.EUSKARA
                ? line.matches("^ebaluazio irizpideak\\s*:?.*$")
                : line.matches("^criterios de evaluacion\\s*:?.*$");
    }

    private boolean isNoise(String line) {
        return line.matches("^\\d+$") || line.matches("(?iu)^orrialdea\\s+\\d+.*$")
                || line.matches("(?iu)^p[aá]gina\\s+\\d+.*$");
    }

    private boolean isModuleMetadata(String line) {
        return line.matches("^(?:\\d+\\s*[.)]\\s*)?(?:lanbide modulua|modulo profesional|heziketa zikloa|ciclo formativo|"
                + "maila|nivel|lanbide arloa|familia profesional|iraupena|duracion|kurtsoa|curso|"
                + "kreditu kop|creditos|irakasleen espezialitatea|especialidad del profesorado|"
                + "modulu mota|tipo de modulo|helburu orokorrak|objetivos generales)\\s*:.*$");
    }

    private boolean isSpuriousModuleLabel(String value) {
        String compact = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");
        return compact.contains("lanbidemodulua") || compact.contains("moduloprofesional");
    }

    private String normalize(String value) {
        String withoutMarks = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return withoutMarks.toLowerCase(Locale.ROOT).replace('-', ' ').replace('–', ' ').replace('—', ' ')
                .replaceAll("\\s+", " ").strip();
    }
}
