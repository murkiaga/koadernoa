package com.koadernoa.app.funtzionalitateak.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedImportRequest;
import com.koadernoa.app.funtzionalitateak.admin.seed.dto.SeedImportResult;
import com.koadernoa.app.funtzionalitateak.admin.seed.service.KatalogoAkademikoaSeedService;
import com.koadernoa.app.objektuak.konfigurazioa.service.AplikazioAukeraService;
import com.koadernoa.app.security.AuthProviderStatusService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AplikazioAukeraService aukService;
    private final AuthProviderStatusService statusService;
    private final KatalogoAkademikoaSeedService katalogoAkademikoaSeedService;

    @Value("${koadernoa.uploads.dir:uploads}")
    private String baseDir;

    @Value("${koadernoa.uploads.logo-subdir:logoa}")
    private String logoSubdir;
    @Value("${koadernoa.uploads.favicon-subdir:favicon}")
    private String faviconSubdir;

    @Value("${koadernoa.txostenak.md6309.path:src/main/resources/templates/txostenak/MD6309-falten-jakinarazpena.dotx}")
    private String md6309TxostenPath;

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
        String faviconUrl = aukService.get(AplikazioAukeraService.APP_FAVICON_URL, "");
        model.addAttribute("faviconUrl", faviconUrl);
        model.addAttribute("faviconBadago", faviconUrl != null && !faviconUrl.isBlank());
        
        model.addAttribute("googleEnabled", statusService.isGoogleEnabled());
        model.addAttribute("googleConfigured", statusService.isGoogleConfigured());
        model.addAttribute("ldapEnabled", statusService.isLdapEnabled());
        model.addAttribute("ldapConfigured", statusService.isLdapConfigured());

        model.addAttribute("md6309TxostenaBadago", Files.exists(getMd6309Path()));
        model.addAttribute("seedEgoera", katalogoAkademikoaSeedService.kalkulatuEgoera());
        model.addAttribute("seedImportRequest", new SeedImportRequest());

        return "admin/index";
    }

    private Path getMd6309Path() {
        return Paths.get(md6309TxostenPath).toAbsolutePath().normalize();
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

    @PostMapping("/favicon")
    public String uploadFavicon(@RequestParam("faviconFile") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) return "redirect:/admin/?error=Fitxategia%20hutsa%20da";
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CT.contains(ct)) {
            return "redirect:/admin/?error=Onartutako%20formatuak:%20PNG%20edo%20JPG";
        }
        var img = ImageIO.read(file.getInputStream());
        if (img == null) return "redirect:/admin/?error=Irudi%20formatu%20baliogabea";
        if (img.getWidth() > 32 || img.getHeight() > 32) {
            return "redirect:/admin/?error=Faviconak%20gehienez%2032x32%20izan%20behar%20du";
        }

        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        Path faviconDir = root.resolve(faviconSubdir).normalize();
        Files.createDirectories(faviconDir);

        String ext = "image/jpeg".equals(ct) ? "jpg" : "png";
        String filename = "favicon." + ext;
        Path target = faviconDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String faviconUrl = "/uploads/" + faviconSubdir + "/" + filename + "?v=" + System.currentTimeMillis();
        aukService.set(AplikazioAukeraService.APP_FAVICON_URL, faviconUrl);
        return "redirect:/admin/?success=Favicon%20gordeta";
    }

    @PostMapping("/favicon/ezabatu")
    public String deleteFavicon() throws IOException {
        String faviconUrl = aukService.get(AplikazioAukeraService.APP_FAVICON_URL, "");
        Path root = Paths.get(baseDir).toAbsolutePath().normalize();
        Path faviconDir = root.resolve(faviconSubdir).normalize();
        if (faviconUrl != null && !faviconUrl.isBlank()) {
            String clean = faviconUrl.split("\\?")[0];
            String filename = StringUtils.getFilename(clean);
            if (filename != null) Files.deleteIfExists(faviconDir.resolve(filename));
        }
        aukService.set(AplikazioAukeraService.APP_FAVICON_URL, "");
        return "redirect:/admin/?success=Favicon%20ezabatuta";
    }

    @PostMapping("/auth-providers")
    public String saveAuthProviders(@RequestParam(name = "googleEnabled", required = false) String googleEnabledParam,
                                    @RequestParam(name = "ldapEnabled", required = false) String ldapEnabledParam) {

        boolean googleEnabled = "on".equalsIgnoreCase(googleEnabledParam);
        boolean ldapEnabled = "on".equalsIgnoreCase(ldapEnabledParam);

        boolean googleConfigured = statusService.isGoogleConfigured();
        boolean ldapConfigured = statusService.isLdapConfigured();

        if (googleEnabled && !googleConfigured) {
            return "redirect:/admin/?error=Google%20ez%20dago%20konfiguratuta";
        }
        if (ldapEnabled && !ldapConfigured) {
            return "redirect:/admin/?error=LDAP%20ez%20dago%20konfiguratuta";
        }
        if (!googleEnabled && !ldapEnabled) {
            return "redirect:/admin/?error=Gutxienez%20autentikazio%20mota%20bat%20aktibo%20egon%20behar%20da";
        }

        aukService.setBool(AplikazioAukeraService.AUTH_GOOGLE_ENABLED, googleEnabled);
        aukService.setBool(AplikazioAukeraService.AUTH_LDAP_ENABLED, ldapEnabled);

        return "redirect:/admin/?success=Autentikazio%20aukerak%20eguneratuta";
    }

    @GetMapping("/txostenak/md6309")
    public ResponseEntity<Resource> deskargatuMd6309() throws IOException {
        Path target = getMd6309Path();
        if (!Files.exists(target)) {
            return ResponseEntity.notFound().build();
        }

        InputStream input = Files.newInputStream(target);
        Resource resource = new InputStreamResource(input);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"MD6309-falten-jakinarazpena.dotx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.template"))
                .contentLength(Files.size(target))
                .body(resource);
    }

    @PostMapping("/txostenak/md6309")
    public String uploadMd6309(@RequestParam("txostenFile") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "redirect:/admin/?tab=txostenak&error=Fitxategia%20hutsa%20da";
        }

        String jatorrizkoIzena = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(jatorrizkoIzena);
        if (ext == null || !"dotx".equalsIgnoreCase(ext)) {
            return "redirect:/admin/?tab=txostenak&error=Onartutako%20fitxategi%20mota%20bakarra%20.DOTX%20da";
        }

        Path target = getMd6309Path();
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return "redirect:/admin/?tab=txostenak&success=Txostena%20eguneratuta";
    }

    @PostMapping("/seed/kargatu")
    public String kargatuSeed(@ModelAttribute SeedImportRequest request) {
        try {
            SeedImportResult result = katalogoAkademikoaSeedService.inportatu(request);
            return "redirect:/admin/?tab=seed&success=" + urlEncode(result.successMessage());
        } catch (IllegalArgumentException e) {
            return "redirect:/admin/?tab=seed&error=" + urlEncode(e.getMessage());
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
