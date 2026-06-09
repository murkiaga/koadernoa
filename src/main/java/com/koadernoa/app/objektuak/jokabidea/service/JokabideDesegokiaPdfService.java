package com.koadernoa.app.objektuak.jokabidea.service;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
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
        c.setVariable("herriaEtaData", (herria == null || herria.isBlank() ? "" : herria.trim() + ", ") + j.getData().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        c.setVariable("irakaslea", j.getIrakaslea().getIzena());
        String html = templateEngine.process("pdf/jokabide-desegokia", c);
        try (OutputStream os = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(os).run();
        } catch (Exception e) { Files.deleteIfExists(target); throw new IOException("Ezin izan da PDF dokumentua sortu.", e); }
        return new SortutakoPdfa(target.toString(), filename);
    }
    public void ezabatuIsilean(String path) { if (path != null) try { Files.deleteIfExists(Paths.get(path)); } catch (IOException ignored) {} }
    private String ikasleIzena(JokabideDesegokia j) { var i=j.getIkaslea(); return ((i.getIzena()==null?"":i.getIzena())+" "+(i.getAbizena1()==null?"":i.getAbizena1())+" "+(i.getAbizena2()==null?"":i.getAbizena2())).trim(); }
    private String maila(JokabideDesegokia j) { var m=j.getModuloa().getMaila(); var t=j.getModuloa().getTaldea(); String base=m==null?"":(m.getIzena()==null?m.getKodea():m.getIzena()); return t!=null&&t.getIzena()!=null?base+" - "+t.getIzena():base; }
    public record SortutakoPdfa(String path, String filename) {}
}
