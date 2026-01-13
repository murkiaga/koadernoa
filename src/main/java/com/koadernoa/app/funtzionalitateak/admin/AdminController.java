package com.koadernoa.app.funtzionalitateak.admin;

import java.io.IOException;
import java.nio.file.*;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AplikazioAukeraService aukService;

    @Value("${koadernoa.uploads.dir:uploads}")
    private String baseDir;

    @Value("${koadernoa.uploads.logo-subdir:logoa}")
    private String logoSubdir;

    private static final Set<String> ALLOWED_CT = Set.of("image/png", "image/jpeg");
    private static final long MAX_BYTES = 300_000; // 300KB

    @GetMapping({"", "/"})
    public String editForm(Model model) {
        model.addAttribute("ebal1Kolore", aukService.get(AplikazioAukeraService.EBAL1_KOLORE, "#b3d9ff"));
        model.addAttribute("ebal2Kolore", aukService.get(AplikazioAukeraService.EBAL2_KOLORE, "#ffd699"));
        model.addAttribute("ebal3Kolore", aukService.get(AplikazioAukeraService.EBAL3_KOLORE, "#b2f2bb"));

        String logoUrl = aukService.get(AplikazioAukeraService.APP_LOGO_URL, "");
        model.addAttribute("logoUrl", logoUrl);
        model.addAttribute("logoBadago", logoUrl != null && !logoUrl.isBlank());

        return "admin/index";
    }

    @PostMapping("/ebalu-koloreak")
    public String saveKoloreak(@RequestParam String ebal1Kolore,
                              @RequestParam String ebal2Kolore,
                              @RequestParam String ebal3Kolore) {

        aukService.set(AplikazioAukeraService.EBAL1_KOLORE, ebal1Kolore);
        aukService.set(AplikazioAukeraService.EBAL2_KOLORE, ebal2Kolore);
        aukService.set(AplikazioAukeraService.EBAL3_KOLORE, ebal3Kolore);

        return "redirect:/admin/?success=Koloreak%20gordeta";
    }

    @PostMapping("/logoa")
    public String uploadLogoa(@RequestParam("logoFile") MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            return "redirect:/admin/?error=Fitxategia%20hutsa%20da";
        }
        if (file.getSize() > MAX_BYTES) {
            return "redirect:/admin/?error=Fitxategia%20handiegia%20da%20(gehienez%20300KB)";
        }

        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CT.contains(ct)) {
            return "redirect:/admin/?error=Onartutako%20formatuak:%20PNG%20edo%20JPG";
        }

        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        Path logoDir = root.resolve(logoSubdir).normalize();
        Files.createDirectories(logoDir);

        String ext = "png";
        if ("image/jpeg".equals(ct)) ext = "jpg";

        String filename = "logo." + ext;
        Path target = logoDir.resolve(filename).normalize();

        // fitxategia ordezkatu
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        // Cache noStore daukazunez, ?v=... ez da derrigorrezkoa, baina uzten dizut
        String logoUrl = "/uploads/" + logoSubdir + "/" + filename + "?v=" + System.currentTimeMillis();
        aukService.set(AplikazioAukeraService.APP_LOGO_URL, logoUrl);

        return "redirect:/admin/?success=Logoa%20gordeta";
    }

    @PostMapping("/logoa/ezabatu")
    public String deleteLogoa() throws IOException {
        String logoUrl = aukService.get(AplikazioAukeraService.APP_LOGO_URL, "");

        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        Path logoDir = root.resolve(logoSubdir).normalize();

        if (logoUrl != null && !logoUrl.isBlank()) {
            String clean = logoUrl.split("\\?")[0]; // ?v=...
            String filename = StringUtils.getFilename(clean); // logo.png/jpg
            if (filename != null) {
                Files.deleteIfExists(logoDir.resolve(filename));
            }
        }

        aukService.set(AplikazioAukeraService.APP_LOGO_URL, "");
        return "redirect:/admin/?success=Logoa%20ezabatuta";
    }
}
