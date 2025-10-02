package com.koadernoa.app.modulua.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.modulua.entitateak.Ikaslea;
import com.koadernoa.app.modulua.repository.IkasleaRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class IkasleArgazkiService {

    @Value("${koadernoa.uploads.dir}")
    private String uploadRoot;

    @Value("${koadernoa.uploads.ikasleak-subdir:ikasleak}")
    private String ikasleSubdir;

    private final IkasleaRepository ikasleaRepository;

    public String gordeArgazkia(Long ikasleId, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Fitxategia hutsik dago.");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Irudi-mota baliogabea.");
        }

        // Onartutako luzapen sinpleak (JPEG/PNG/WebP)
        String ext = ".jpg";
        if (contentType.contains("png")) ext = ".png";
        else if (contentType.contains("webp")) ext = ".webp";

        Path dir = Path.of(uploadRoot, ikasleSubdir, String.valueOf(ikasleId));
        Files.createDirectories(dir);

        // Izendapena: originala ez zaigu axola; gordeko dugu "profile" izenarekin
        Path target = dir.resolve("profile" + ext);

        // Ezabatu aurreko profile.* existitzen bada
        try (var s = Files.list(dir)) {
            s.filter(p -> p.getFileName().toString().startsWith("profile."))
             .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }

        // Gorde
        file.transferTo(target.toFile());

        // DB-an path publikoa (handler-ak /uploads/** zerbitzatzen du)
        String publicPath = "/uploads/" + ikasleSubdir + "/" + ikasleId + "/profile" + ext;

        Ikaslea ikaslea = ikasleaRepository.findById(ikasleId)
                .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da existitzen: " + ikasleId));
        ikaslea.setArgazkiPath(publicPath);
        ikasleaRepository.save(ikaslea);

        return publicPath;
    }
}