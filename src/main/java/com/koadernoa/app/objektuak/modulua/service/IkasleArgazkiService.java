package com.koadernoa.app.objektuak.modulua.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.imageio.ImageIO;

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
        
        // Konprimatu JPEG handiegia bada
        if (ext.equals(".jpg") || ext.equals(".jpeg")) {
            konprimatuIrudia(target, target, 1600, 0.8); // max 1600 px zabaleran, 80 % kalitatea
        }

        // DBn URL erlatiboa gorde
        String baseUrl = "/uploads/" + subdir + "/" + hna + ext;
        ik.setArgazkiPath(baseUrl);
        ikasleaRepository.save(ik);
        
        // 2) Bezeroari bertsionatua itzuli (cache-buster)
        long lastMod = Files.getLastModifiedTime(target).toMillis();
        return baseUrl + "?v=" + lastMod;
    }
    
    
    private void konprimatuIrudia(Path jatorria, Path helburua, double maxZabalera, double kalitatea) throws IOException {
        BufferedImage src = ImageIO.read(jatorria.toFile());
        if (src == null) return;

        // Jatorrizko neurriak
        int w = src.getWidth();
        int h = src.getHeight();

        // Handiegia bada soilik murriztu
        if (w > maxZabalera) {
            double ratio = maxZabalera / w;
            int berriaW = (int) (w * ratio);
            int berriaH = (int) (h * ratio);

            BufferedImage txikiagoa = new BufferedImage(berriaW, berriaH, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = txikiagoa.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(src, 0, 0, berriaW, berriaH, null);
            g2.dispose();

            // Konpresioarekin gorde
            try (OutputStream out = Files.newOutputStream(helburua)) {
                javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);
                javax.imageio.plugins.jpeg.JPEGImageWriteParam params = new javax.imageio.plugins.jpeg.JPEGImageWriteParam(null);
                params.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality((float) kalitatea); // 0.0–1.0 arteko tartea
                writer.write(null, new javax.imageio.IIOImage(txikiagoa, null, null), params);
                writer.dispose();
                ios.close();
            }
        }
    }
}