package com.koadernoa.app.objektuak.jokabidea.service;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JokabideDesegokiaPdfService {
    private final TemplateEngine templateEngine;
    @Value("${koadernoa.uploads.dir:uploads}") private String uploadsDir;
    @Value("${koadernoa.uploads.jokabide-desegokiak-subdir:jokabide-desegokiak}") private String subdir;
    @Value("${koadernoa.txostenak.jokabide-desegokia.path:uploads/txostenak/jokabide-desegokia.html}") private String txantiloiPertsonalizatuaPath;
    @Value("${koadernoa.herria:}") private String herria;

    public SortutakoPdfa sortu(JokabideDesegokia j) throws IOException {
        String ikasturtea = j.getKoadernoa().getEgutegia().getIkasturtea().getIzena().replaceAll("[^A-Za-z0-9._-]", "_");
        Path dir = Paths.get(uploadsDir, subdir, ikasturtea, String.valueOf(j.getKoadernoa().getId())).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = "jokabide-desegokia-" + j.getIkaslea().getId() + "-" + j.getData() + "-" + java.util.UUID.randomUUID() + ".pdf";
        Path target = dir.resolve(filename);
        Context c = new Context(new Locale("eu", "ES"));
        c.setVariable("ikaslea", ikasleIzena(j));
        c.setVariable("maila", maila(j));
        c.setVariable("portaeraArrazoia", j.getPortaeraArrazoia().getKodea() + " - " + j.getPortaeraArrazoia().getTestua());
        c.setVariable("deskribapenZehatza", j.getDeskribapenZehatza());
        c.setVariable("neurriZuzentzailea", j.getNeurriZuzentzailea().getKodea() + " - " + j.getNeurriZuzentzailea().getTestua());
        c.setVariable("herriaEtaData", herriaEtaData(j));
        c.setVariable("irakaslea", j.getIrakaslea().getIzena());
        String html = errendatuHtml(c);
        try (OutputStream os = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(os).run();
        } catch (Exception e) { Files.deleteIfExists(target); throw new IOException("Ezin izan da PDF dokumentua sortu.", e); }
        return new SortutakoPdfa(target.toString(), filename);
    }
    private String errendatuHtml(Context context) throws IOException {
        Path txantiloia = Paths.get(txantiloiPertsonalizatuaPath).toAbsolutePath().normalize();
        if (!Files.exists(txantiloia)) {
            return templateEngine.process("pdf/jokabide-desegokia", context);
        }

        return ordezkatuPlaceholderak(Files.readString(txantiloia), context);
    }

    private String ordezkatuPlaceholderak(String html, Context context) {
        String emaitza = html;
        for (String izena : context.getVariableNames()) {
            Object balioa = context.getVariable(izena);
            String balioEscaped = escapeHtml(balioa == null ? "" : balioa.toString());
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

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public void ezabatuIsilean(String path) { if (path != null) try { Files.deleteIfExists(Paths.get(path)); } catch (IOException ignored) {} }
    private String ikasleIzena(JokabideDesegokia j) { var i=j.getIkaslea(); return ((i.getIzena()==null?"":i.getIzena())+" "+(i.getAbizena1()==null?"":i.getAbizena1())+" "+(i.getAbizena2()==null?"":i.getAbizena2())).trim(); }
    private String maila(JokabideDesegokia j) { var m=j.getModuloa().getMaila(); var t=j.getModuloa().getTaldea(); String base=m==null?"":(m.getIzena()==null?m.getKodea():m.getIzena()); return t!=null&&t.getIzena()!=null?base+" - "+t.getIzena():base; }
    private String herriaEtaData(JokabideDesegokia j) {
        String[] hilabeteak = { "urtarril", "otsail", "martxo", "apiril", "maiatz", "ekain",
                "uztail", "abuztu", "irail", "urri", "azaro", "abendu" };
        String kokapena = herria == null || herria.isBlank() ? "………….…………." : herria.trim();
        var data = j.getData();
        return kokapena + "(e)n, " + data.getYear() + "(e)ko "
                + hilabeteak[data.getMonthValue() - 1] + "aren " + data.getDayOfMonth() + "(e)(a)n";
    }
    public record SortutakoPdfa(String path, String filename) {}
}
