package com.koadernoa.app.objektuak.ordutegiak.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class KronowinOrdutegiXmlParser {
    public KronowinData parse(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();

            Map<String, KronowinIrakaslea> irakasleak = new HashMap<>();
            NodeList proff = document.getElementsByTagName("PROFF");
            for (int i = 0; i < proff.getLength(); i++) {
                Element e = (Element) proff.item(i);
                String kodea = attr(e, "ABREV");
                if (!kodea.isBlank()) {
                    irakasleak.put(key(kodea), new KronowinIrakaslea(kodea, attr(e, "NOMBRE"), attr(e, "DEPART"), attr(e, "EMAIL")));
                }
            }

            Map<String, String> moduluIzenak = new HashMap<>();
            NodeList nomasigf = document.getElementsByTagName("NOMASIGF");
            for (int i = 0; i < nomasigf.getLength(); i++) {
                Element e = (Element) nomasigf.item(i);
                String kodea = firstNonBlank(attr(e, "ABREV"), attr(e, "ASIG"));
                if (!kodea.isBlank()) {
                    moduluIzenak.put(key(kodea), attr(e, "NOMBRE"));
                }
            }

            Map<String, String> gelaIzenak = new HashMap<>();
            NodeList aulaf = document.getElementsByTagName("AULAF");
            for (int i = 0; i < aulaf.getLength(); i++) {
                Element e = (Element) aulaf.item(i);
                String kodea = firstNonBlank(attr(e, "ABREV"), attr(e, "AULA"));
                if (!kodea.isBlank()) {
                    gelaIzenak.put(key(kodea), attr(e, "NOMBRE"));
                }
            }

            List<KronowinSolucf> solucf = new ArrayList<>();
            NodeList nodes = document.getElementsByTagName("SOLUCF");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element e = (Element) nodes.item(i);
                solucf.add(new KronowinSolucf(
                        attr(e, "PROF"), attr(e, "ASIG"), attr(e, "AULA"), attr(e, "CODGRUPO"),
                        parseInt(attr(e, "DIA")), parseInt(attr(e, "HORA")), parseInt(attr(e, "SESIONES")),
                        attr(e, "CURSO"), attr(e, "GRUPO"), attr(e, "NIVEL"), attr(e, "TURNO"),
                        attr(e, "MARCO"), attr(e, "TAREA")));
            }
            return new KronowinData(irakasleak, moduluIzenak, gelaIzenak, solucf);
        } catch (Exception e) {
            throw new IllegalArgumentException("KRONOWIN XML fitxategia ezin izan da irakurri", e);
        }
    }

    static String key(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String attr(Element element, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name).trim() : "";
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "";
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record KronowinData(Map<String, KronowinIrakaslea> irakasleak,
                               Map<String, String> moduluIzenak,
                               Map<String, String> gelaIzenak,
                               List<KronowinSolucf> solucf) {
    }

    public record KronowinIrakaslea(String kodea, String izena, String depart, String email) {
    }

    public record KronowinSolucf(String prof, String asig, String aula, String codgrupo,
                                 Integer dia, Integer hora, Integer sesiones, String curso,
                                 String grupo, String nivel, String turno, String marco, String tarea) {
    }
}
