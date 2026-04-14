package com.koadernoa.app.objektuak.koadernoak.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FaltenJakinarazpenPdfService {

    private static final Locale EUSKARA = new Locale("eu", "ES");
    private static final Locale GAZTELANIA = new Locale("es", "ES");
    private static final Pattern LIBREOFFICE_PLACEHOLDER = Pattern.compile("Haga clic o pulse aquí para escribir texto\\.?");

    @Value("${koadernoa.falten-jakinarazpena.template:classpath:/templates/falten-jakinarazpena.dotx}")
    private Resource txantiloia;

    public byte[] sortuPdf(FaltenJakinarazpenaData data) {
        if (!txantiloia.exists()) {
            throw new IllegalStateException("Ez da aurkitu falten jakinarazpenaren .dotx txantiloia.");
        }

        try (InputStream in = txantiloia.getInputStream();
             XWPFDocument document = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Map<String, String> balioak = data.toMap();
            ordezkatuDokumentuan(document, balioak, data.toOrderedValues());
            PdfConverter.getInstance().convert(document, out, PdfOptions.create());
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Errorea PDF jakinarazpena sortzean", e);
        }
    }

    private void ordezkatuDokumentuan(XWPFDocument doc, Map<String, String> balioak, java.util.List<String> ordenekoBalioak) {
        AtomicInteger placeholderIdx = new AtomicInteger(0);

        for (XWPFParagraph p : doc.getParagraphs()) {
            ordezkatuParagrafoan(p, balioak, ordenekoBalioak, placeholderIdx);
        }

        for (XWPFTable t : doc.getTables()) {
            t.getRows().forEach(r -> r.getTableCells().forEach(c ->
                c.getParagraphs().forEach(p -> ordezkatuParagrafoan(p, balioak, ordenekoBalioak, placeholderIdx))
            ));
        }

        doc.getHeaderList().forEach(h -> h.getParagraphs().forEach(p -> ordezkatuParagrafoan(p, balioak, ordenekoBalioak, placeholderIdx)));
        doc.getFooterList().forEach(f -> f.getParagraphs().forEach(p -> ordezkatuParagrafoan(p, balioak, ordenekoBalioak, placeholderIdx)));
    }

    private void ordezkatuParagrafoan(
        XWPFParagraph p,
        Map<String, String> balioak,
        java.util.List<String> ordenekoBalioak,
        AtomicInteger placeholderIdx
    ) {
        String testua = p.getText();
        if (testua == null || testua.isBlank()) {
            return;
        }

        String berria = testua;
        for (Map.Entry<String, String> e : balioak.entrySet()) {
            String k = e.getKey();
            String v = e.getValue();
            berria = berria
                .replace("${" + k + "}", v)
                .replace("{{" + k + "}}", v)
                .replace("<<" + k + ">>", v);
        }

        berria = ordezkatuLibreOfficePlaceholderrak(berria, ordenekoBalioak, placeholderIdx);

        if (!berria.equals(testua)) {
            int runs = p.getRuns().size();
            for (int i = runs - 1; i >= 0; i--) {
                p.removeRun(i);
            }
            p.createRun().setText(berria);
        }
    }

    private String ordezkatuLibreOfficePlaceholderrak(String testua, java.util.List<String> ordenekoBalioak, AtomicInteger placeholderIdx) {
        Matcher matcher = LIBREOFFICE_PLACEHOLDER.matcher(testua);
        StringBuffer sb = new StringBuffer();
        boolean ordezkatuDa = false;

        while (matcher.find()) {
            int idx = placeholderIdx.getAndIncrement();
            if (idx >= ordenekoBalioak.size()) {
                break;
            }
            String balioa = Objects.toString(ordenekoBalioak.get(idx), "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(balioa));
            ordezkatuDa = true;
        }

        if (!ordezkatuDa) {
            return testua;
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    public record FaltenJakinarazpenaData(
        String taldea,
        String ikasleaIzenAbizenak,
        int faltaOrduak,
        String modulua,
        int faltaPortzentaia,
        int urteaAzkenBi,
        String hilabeteaEu,
        int eguna,
        String deskargatzailea,
        String hilabeteaEs
    ) {
        public static FaltenJakinarazpenaData of(
            String taldea,
            String ikasleaIzenAbizenak,
            int faltaOrduak,
            String modulua,
            double faltaPortzentaia,
            int urtea,
            LocalDate data,
            String deskargatzailea
        ) {
            return new FaltenJakinarazpenaData(
                balioa(taldea),
                balioa(ikasleaIzenAbizenak),
                faltaOrduak,
                balioa(modulua),
                (int) Math.round(faltaPortzentaia),
                urtea % 100,
                data.getMonth().getDisplayName(TextStyle.FULL, EUSKARA),
                data.getDayOfMonth(),
                balioa(deskargatzailea),
                data.getMonth().getDisplayName(TextStyle.FULL, GAZTELANIA)
            );
        }

        public Map<String, String> toMap() {
            return Map.ofEntries(
                Map.entry("1", taldea),
                Map.entry("2", ikasleaIzenAbizenak),
                Map.entry("3", String.valueOf(faltaOrduak)),
                Map.entry("4", modulua),
                Map.entry("5", String.valueOf(faltaPortzentaia)),
                Map.entry("6", String.valueOf(urteaAzkenBi)),
                Map.entry("7", hilabeteaEu),
                Map.entry("8", String.valueOf(eguna)),
                Map.entry("9", deskargatzailea),
                Map.entry("10", ikasleaIzenAbizenak),
                Map.entry("11", taldea),
                Map.entry("12", String.valueOf(faltaOrduak)),
                Map.entry("13", modulua),
                Map.entry("14", String.valueOf(faltaPortzentaia)),
                Map.entry("15", String.valueOf(eguna)),
                Map.entry("16", hilabeteaEs),
                Map.entry("17", String.valueOf(urteaAzkenBi)),
                Map.entry("18", deskargatzailea)
            );
        }

        public java.util.List<String> toOrderedValues() {
            java.util.List<String> values = new ArrayList<>();
            values.add(taldea);
            values.add(ikasleaIzenAbizenak);
            values.add(String.valueOf(faltaOrduak));
            values.add(modulua);
            values.add(String.valueOf(faltaPortzentaia));
            values.add(String.valueOf(urteaAzkenBi));
            values.add(hilabeteaEu);
            values.add(String.valueOf(eguna));
            values.add(deskargatzailea);
            values.add(ikasleaIzenAbizenak);
            values.add(taldea);
            values.add(String.valueOf(faltaOrduak));
            values.add(modulua);
            values.add(String.valueOf(faltaPortzentaia));
            values.add(String.valueOf(eguna));
            values.add(hilabeteaEs);
            values.add(String.valueOf(urteaAzkenBi));
            values.add(deskargatzailea);
            return values;
        }

        private static String balioa(String testua) {
            return testua == null ? "" : testua;
        }
    }
}
