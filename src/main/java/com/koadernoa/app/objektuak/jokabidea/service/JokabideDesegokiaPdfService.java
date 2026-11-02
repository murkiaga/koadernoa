package com.koadernoa.app.objektuak.jokabidea.service;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JokabideDesegokiaPdfService {
    private final TemplateEngine templateEngine;
    private final JokabideDesegokiaTxantiloiService txantiloiService;
    private final AplikazioAukeraService aplikazioAukeraService;
    @Value("${koadernoa.uploads.dir:uploads}") private String uploadsDir;
    @Value("${koadernoa.uploads.jokabide-desegokiak-subdir:jokabide-desegokiak}") private String subdir;

    public SortutakoPdfa sortu(JokabideDesegokia j) throws IOException {
        String ikasturtea = j.getKoadernoa().getEgutegia().getIkasturtea().getIzena().replaceAll("[^A-Za-z0-9._-]", "_");
        Path dir = Paths.get(uploadsDir, subdir, ikasturtea, String.valueOf(j.getKoadernoa().getId())).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = "jokabide-desegokia-" + j.getIkaslea().getId() + "-" + j.getData() + "-" + java.util.UUID.randomUUID() + ".pdf";
        Path target = dir.resolve(filename);
        Context c = new Context(new Locale("eu", "ES"));
        String herria = herriaTestua();
        c.setVariable("ikaslea", ikasleIzena(j));
        c.setVariable("maila", maila(j));
        c.setVariable("portaeraArrazoia", j.getPortaeraArrazoia().getKodea() + " - " + j.getPortaeraArrazoia().getTestua());
        c.setVariable("deskribapenZehatza", j.getDeskribapenZehatza());
        c.setVariable("neurriZuzentzailea", j.getNeurriZuzentzailea().getKodea() + " - " + j.getNeurriZuzentzailea().getTestua());
        c.setVariable("aplikatutakoNeurria", j.getAplikatutakoNeurria() == null ? "" : j.getAplikatutakoNeurria());
        c.setVariable("herria", herria);
        c.setVariable("dataTestua", dataTestua(j));
        c.setVariable("herriaEtaData", herriaEtaData(j, herria));
        c.setVariable("irakaslea", j.getIrakaslea().getIzena());
        String html = errendatuHtml(c);
        try (OutputStream os = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(os).run();
        } catch (Exception e) { Files.deleteIfExists(target); throw new IOException("Ezin izan da PDF dokumentua sortu.", e); }
        return new SortutakoPdfa(target.toString(), filename);
    }
    private String errendatuHtml(Context context) throws IOException {
        if (!txantiloiService.pertsonalizatuaBadago()) {
            return templateEngine.process("pdf/jokabide-desegokia", context);
        }

        return ordezkatuPlaceholderak(txantiloiService.irakurriPertsonalizatua(), context);
    }

    String ordezkatuPlaceholderak(String html, Context context) {
        String emaitza = html;
        for (String izena : context.getVariableNames()) {
            Object balioa = context.getVariable(izena);
            String balioEscaped = escapeHtml(balioa == null ? "" : balioa.toString());
            if (lerroJauziakMantendu(izena)) {
                balioEscaped = balioEscaped.replace("\r\n", "\n")
                        .replace('\r', '\n')
                        .replace("\n", "<br/>");
            }
            String quotedValue = java.util.regex.Matcher.quoteReplacement(balioEscaped);
            String quotedName = java.util.regex.Pattern.quote(izena);

            emaitza = java.util.regex.Pattern
                    .compile("(<[^>]+\\s)th:text=([\"'])\\$\\{" + quotedName + "\\}\\2([^>]*>)(.*?)(</[^>]+>)", java.util.regex.Pattern.DOTALL)
                    .matcher(emaitza)
                    .replaceAll("$1$3" + quotedValue + "$5");
            emaitza = emaitza.replace("[[${" + izena + "}]]", balioEscaped);
            emaitza = emaitza.replace("${" + izena + "}", balioEscaped);
        }
        return emaitza;
    }

    private boolean lerroJauziakMantendu(String izena) {
        return "portaeraArrazoia".equals(izena)
                || "neurriZuzentzailea".equals(izena)
                || "deskribapenZehatza".equals(izena)
                || "aplikatutakoNeurria".equals(izena);
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public void ezabatuIsilean(String path) { if (path != null) try { Files.deleteIfExists(Paths.get(path)); } catch (IOException ignored) {} }
    private String ikasleIzena(JokabideDesegokia j) { var i=j.getIkaslea(); return ((i.getIzena()==null?"":i.getIzena())+" "+(i.getAbizena1()==null?"":i.getAbizena1())+" "+(i.getAbizena2()==null?"":i.getAbizena2())).trim(); }
    private String maila(JokabideDesegokia j) {
        var modulua = j.getModuloa() != null ? j.getModuloa()
                : j.getKoadernoa() != null ? j.getKoadernoa().getModuloa() : null;
        var maila = modulua != null ? modulua.getMaila() : null;
        var taldea = j.getIkaslea() != null && j.getIkaslea().getTaldea() != null
                ? j.getIkaslea().getTaldea()
                : modulua != null ? modulua.getTaldea() : null;
        String base = maila == null ? "" : (maila.getIzena() == null ? maila.getKodea() : maila.getIzena());
        return taldea != null && taldea.getIzena() != null
                ? (base.isBlank() ? taldea.getIzena() : base + " - " + taldea.getIzena())
                : base;
    }
    private String herriaEtaData(JokabideDesegokia j, String herria) {
        return herria + "(e)n, " + dataTestua(j);
    }

    String herriaTestua() {
        String herria = aplikazioAukeraService.get(AplikazioAukeraService.IKASTETXEAREN_HERRIA);
        return herria == null || herria.isBlank() ? "………….…………." : herria.trim();
    }

    private String dataTestua(JokabideDesegokia j) {
        String[] hilabeteak = { "urtarril", "otsail", "martxo", "apiril", "maiatz", "ekain",
                "uztail", "abuztu", "irail", "urri", "azaro", "abendu" };
        var data = j.getData();
        return data.getYear() + "(e)ko " + hilabeteak[data.getMonthValue() - 1]
                + "aren " + data.getDayOfMonth() + "(e)(a)n";
    }
    public record SortutakoPdfa(String path, String filename) {}
}
