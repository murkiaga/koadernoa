package com.koadernoa.app.objektuak.modulua.service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.modulua.entitateak.Ikaslea;
import com.koadernoa.app.objektuak.modulua.repository.IkasleaRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class IkasleArgazkiService {

	@Value("${koadernoa.uploads.dir}")
    private String baseDir;
    @Value("${koadernoa.uploads.ikasleak-subdir:ikasleak}")
    private String subdir;

    private final IkasleaRepository ikasleaRepository;

    public String gordeArgazkia(Long ikasleId, MultipartFile file) throws IOException {
        Ikaslea ik = ikasleaRepository.findById(ikasleId)
                      .orElseThrow(() -> new IllegalArgumentException("Ikaslea ez da existitzen"));
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Fitxategia hutsik");

        // Eduki mota -> luzapena
        String ct = file.getContentType();
        String ext = switch (ct) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Irudi formatua ez da onartzen: " + ct);
        };

        // HNA derrigor
        String hna = ik.getHna();
        if (hna == null || hna.isBlank())
            throw new IllegalStateException("Ikasleak ez dauka HNA baliorik");

        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        Path dir  = root.resolve(subdir).normalize();
        Files.createDirectories(dir);

        // Helburua (gainidaztea onartzen da)
        Path target = dir.resolve(hna + ext).normalize();

        // (aukerakoa) lehen ezabatu beste luzapenekin (formatoa aldatzen bada)
        for (String e : List.of(".jpg",".png",".webp")) {
            if (!e.equals(ext)) {
                Files.deleteIfExists(dir.resolve(hna + e));
            }
        }

        // Gorde
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }

        // DBn URL erlatiboa gorde
        String url = "/uploads/" + subdir + "/" + hna + ext;
        ik.setArgazkiPath(url);
        ikasleaRepository.save(ik);
        return url;
    }
}