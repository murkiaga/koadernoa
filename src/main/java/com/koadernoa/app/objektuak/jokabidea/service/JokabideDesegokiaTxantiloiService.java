package com.koadernoa.app.objektuak.jokabidea.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class JokabideDesegokiaTxantiloiService {
    private final String txantiloiPertsonalizatuaPath;

    public JokabideDesegokiaTxantiloiService(
            @Value("${koadernoa.txostenak.jokabide-desegokia.path:uploads/txostenak/jokabide-desegokia.html}") String txantiloiPertsonalizatuaPath) {
        this.txantiloiPertsonalizatuaPath = txantiloiPertsonalizatuaPath;
    }

    public Path getPertsonalizatuaPath() {
        return Paths.get(txantiloiPertsonalizatuaPath).toAbsolutePath().normalize();
    }

    public boolean pertsonalizatuaBadago() {
        return Files.exists(getPertsonalizatuaPath());
    }

    public String irakurriDefektuzkoa() throws IOException {
        Resource resource = new ClassPathResource("templates/pdf/jokabide-desegokia.html");
        try (InputStream input = resource.getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String irakurriPertsonalizatua() throws IOException {
        return Files.readString(getPertsonalizatuaPath(), StandardCharsets.UTF_8);
    }

    public String irakurriUnekoa() throws IOException {
        return pertsonalizatuaBadago() ? irakurriPertsonalizatua() : irakurriDefektuzkoa();
    }

    public void gordePertsonalizatua(String htmlEdukia) throws IOException {
        Path target = getPertsonalizatuaPath();
        Files.createDirectories(target.getParent());
        Files.writeString(target, htmlEdukia, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void berrezarri() throws IOException {
        Files.deleteIfExists(getPertsonalizatuaPath());
    }
}
