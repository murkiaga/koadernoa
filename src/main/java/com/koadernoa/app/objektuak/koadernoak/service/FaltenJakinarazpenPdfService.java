package com.koadernoa.app.objektuak.koadernoak.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
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
            ordezkatuDokumentuan(document, balioak);
            PdfConverter.getInstance().convert(document, out, PdfOptions.create());
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Errorea PDF jakinarazpena sortzean", e);
        }
    }

    private void ordezkatuDokumentuan(XWPFDocument doc, Map<String, String> balioak) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            ordezkatuParagrafoan(p, balioak);
        }

        for (XWPFTable t : doc.getTables()) {
            t.getRows().forEach(r -> r.getTableCells().forEach(c ->
                c.getParagraphs().forEach(p -> ordezkatuParagrafoan(p, balioak))
            ));
        }

        doc.getHeaderList().forEach(h -> h.getParagraphs().forEach(p -> ordezkatuParagrafoan(p, balioak)));
        doc.getFooterList().forEach(f -> f.getParagraphs().forEach(p -> ordezkatuParagrafoan(p, balioak)));
    }

    private void ordezkatuParagrafoan(XWPFParagraph p, Map<String, String> balioak) {
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

        if (!berria.equals(testua)) {
            int runs = p.getRuns().size();
            for (int i = runs - 1; i >= 0; i--) {
                p.removeRun(i);
            }
            p.createRun().setText(berria);
        }
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

        private static String balioa(String testua) {
            return testua == null ? "" : testua;
        }
    }
}
