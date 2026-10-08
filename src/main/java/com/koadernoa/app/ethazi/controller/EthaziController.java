package com.koadernoa.app.ethazi.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.koadernoa.app.ethazi.dto.EthaziForms.*;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.GaitasunMota;
import com.koadernoa.app.ethazi.service.EthaziService;
import com.koadernoa.app.ethazi.service.IkaskuntzaEmaitzaCsvImportService;
import com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/ethazi")
@RequiredArgsConstructor
public class EthaziController {
    private final EthaziService service;
    private final IkaskuntzaEmaitzaCsvImportService emaitzaCsvImportService;

    @ModelAttribute("hizkuntza")
    Hizkuntza hizkuntza(
            @RequestParam(defaultValue="EUSKARA") Hizkuntza hizkuntza) {
        return hizkuntza;
    }
    @ModelAttribute
    void aukerak(Model model) {
        model.addAttribute("zikloak", service.zikloak());
        model.addAttribute("motak", GaitasunMota.values());
        model.addAttribute("itzulpenHizkuntzak", java.util.List.of(
            com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.EUSKARA,
            com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.GAZTELERA,
            com.koadernoa.app.objektuak.modulua.entitateak.Hizkuntza.INGELERA));
    }
    @GetMapping({"", "/"})
    public String index() { return "redirect:/ethazi/gaitasunak/teknikoak"; }

    @GetMapping({"/gaitasunak", "/gaitasunak/teknikoak"})
    public String gaitasunak(@RequestParam(required = false) Long zikloaId,
            @RequestParam(defaultValue = "TEKNIKOA") GaitasunMota mota, Model model) {
        mota = GaitasunMota.TEKNIKOA;
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("mota", mota);
        model.addAttribute("errubrika", service.errubrika(zikloaId, mota));
        model.addAttribute("curriculum", service.curriculum(zikloaId));
        return "Ethazi/gaitasunak/index";
    }
    @GetMapping("/gaitasunak/zeharkakoak")
    public String zeharkakoGaitasunak(Model model) {
        model.addAttribute("errubrikak", service.zeharkakoErrubrikak());
        return "Ethazi/gaitasunak/zeharkakoak";
    }
    @GetMapping({"/kinielak", "/kinielak/"})
    public String kinielakHelbideZaharra(@RequestParam(required = false) Long zikloaId) {
        return "redirect:/ethazi/kiniela" + (zikloaId == null ? "" : "?zikloaId=" + zikloaId);
    }

    private String errubrikaHelbidea(Long zikloaId, GaitasunMota mota, Hizkuntza h) {
        if (mota == GaitasunMota.ZEHARKAKOA) return "/gaitasunak/zeharkakoak" + (h == Hizkuntza.EUSKARA ? "" : "?hizkuntza=" + h);
        return "/gaitasunak?zikloaId=" + zikloaId + "&mota=TEKNIKOA" + (h == Hizkuntza.EUSKARA ? "" : "&hizkuntza=" + h);
    }
    private String errubrikaHelbidea(com.koadernoa.app.ethazi.entitateak.gaitasunak.Gaitasuna g, Hizkuntza h) {
        return errubrikaHelbidea(g.getZikloa() == null ? null : g.getZikloa().getId(), g.getMota(), h);
    }

    @PostMapping("/errubrika/mailak/berria")
    public String errubrikaMailaGehitu(@RequestParam Long zikloaId, @RequestParam GaitasunMota mota,
            RedirectAttributes flash, @RequestParam(defaultValue="EUSKARA") Hizkuntza hizkuntza) {
        try {
            Long mailaId = service.gehituErrubrikaMaila(zikloaId, mota);
            flash.addFlashAttribute("success", "Maila berria gehitu da. Izena goiburuan alda dezakezu.");
            flash.addFlashAttribute("addedMailaId", mailaId);
            return "redirect:/ethazi" + errubrikaHelbidea(zikloaId, mota, hizkuntza) + "#maila-" + mailaId;
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", mezua(ex));
            return "redirect:/ethazi" + errubrikaHelbidea(zikloaId, mota, hizkuntza);
        }
    }

    @PostMapping("/errubrika/mailak/{mailaId}/ezabatu")
    public String errubrikaMailaEzabatu(@PathVariable Long mailaId, @RequestParam Long zikloaId,
            @RequestParam GaitasunMota mota, RedirectAttributes flash, @RequestParam(defaultValue="EUSKARA") Hizkuntza hizkuntza) {
        return egin(() -> service.ezabatuErrubrikaMaila(zikloaId, mota, mailaId), "Maila ezabatu da.",
                errubrikaHelbidea(zikloaId, mota, hizkuntza), flash);
    }

    @PostMapping("/errubrika/mailak/{mailaId}/editatu")
    public String errubrikaMailaIzendatu(@PathVariable Long mailaId, @RequestParam Long zikloaId,
            @RequestParam GaitasunMota mota, @ModelAttribute MailaForm form, BindingResult binding,
            Model model, RedirectAttributes flash, @RequestParam(defaultValue="EUSKARA") Hizkuntza hizkuntza) {
        try {
            balidatu(binding); service.izendatuErrubrikaMaila(zikloaId, mota, mailaId, form);
            flash.addFlashAttribute("success", "Mailaren izena gorde da.");
            return "redirect:/ethazi" + errubrikaHelbidea(zikloaId, mota, hizkuntza) + "#maila-" + mailaId;
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            model.addAttribute("failedMaila", form);
            model.addAttribute("failedMailaId", mailaId);
            return gaitasunak(zikloaId, mota, model);
        }
    }

    @PostMapping({"/errubrika/gaitasunak/{id}/mailak/{mailaId}/adierazleak/berria",
            "/errubrika/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/editatu"})
    public String errubrikaAdierazleGorde(@PathVariable Long id, @PathVariable Long mailaId,
            @PathVariable(required = false) Long adierazleaId, @ModelAttribute AdierazleaForm form,
            BindingResult binding, Model model, RedirectAttributes flash, @RequestParam(defaultValue="EUSKARA") Hizkuntza hizkuntza) {
        var g = service.gaitasuna(id);
        try {
            balidatu(binding); service.gordeErrubrikaAdierazlea(id, mailaId, adierazleaId, form);
            flash.addFlashAttribute("success", "Lorpen-adierazlea gorde da.");
            return "redirect:/ethazi" + errubrikaHelbidea(g, hizkuntza) + "#gelaxka-" + id + "-" + mailaId;
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            model.addAttribute("failedForm", form);
            model.addAttribute("failedGaitasunaId", id);
            model.addAttribute("failedMailaId", mailaId);
            model.addAttribute("failedAdierazleaId", adierazleaId);
            if (g.getMota() == GaitasunMota.ZEHARKAKOA) return zeharkakoGaitasunak(model);
            return gaitasunak(g.getZikloa().getId(), g.getMota(), model);
        }
    }

    @PostMapping("/errubrika/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/ezabatu")
    public String errubrikaAdierazleEzabatu(@PathVariable Long id, @PathVariable Long mailaId,
            @PathVariable Long adierazleaId, RedirectAttributes flash, @RequestParam(defaultValue="EUSKARA") Hizkuntza hizkuntza) {
        var g = service.gaitasuna(id);
        return egin(() -> service.ezabatuErrubrikaAdierazlea(id, mailaId, adierazleaId), "Lorpen-adierazlea ezabatu da.",
                errubrikaHelbidea(g, hizkuntza) + "#gelaxka-" + id + "-" + mailaId, flash);
    }

    @PostMapping("/errubrika/gaitasunak/{id}/mailak/{mailaId}/adierazleak/berrordenatu")
    @ResponseBody
    public ResponseEntity<Map<String, String>> adierazleBerrordenatu(@PathVariable Long id, @PathVariable Long mailaId,
            @RequestParam List<Long> adierazleaIds) {
        try {
            service.berrordenatuAdierazleak(id, mailaId, adierazleaIds);
            return ResponseEntity.ok(Map.of("message", "Lorpen-adierazleen ordena gorde da."));
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", mezua(ex)));
        }
    }

    @GetMapping("/gaitasunak/berria")
    public String gaitasunBerria(@RequestParam(required = false) Long zikloaId,
            @RequestParam(defaultValue = "TEKNIKOA") GaitasunMota mota, Model model) {
        return gaitasunForm(null, service.gaitasunaForm(null, zikloaId, mota), model);
    }
    @GetMapping("/gaitasunak/zeharkakoak/berria")
    public String zeharkakoGaitasunBerria(Model model) {
        return gaitasunForm(null, service.gaitasunaForm(null, null, GaitasunMota.ZEHARKAKOA), model);
    }
    @GetMapping("/gaitasunak/{id}/editatu")
    public String gaitasunEditatu(@PathVariable Long id, Model model) {
        return gaitasunForm(id, service.gaitasunaForm(id, null, null), model);
    }
    private String gaitasunForm(Long id, GaitasunaForm form, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("form", form);
        var gaitasuna = id == null ? null : service.gaitasuna(id);
        model.addAttribute("eredua", gaitasuna != null && gaitasuna.getEredua() != null
                ? gaitasuna.getEredua() : service.eredua(form.getZikloaId(), form.getMota()));
        model.addAttribute("gaitasuna", gaitasuna);
        model.addAttribute("curriculum", service.curriculum(form.getZikloaId()));
        model.addAttribute("mailaIzenak", gaitasuna == null ? service.mailaIzenak(form.getZikloaId(), form.getMota()) : service.mailaIzenak(gaitasuna));
        return "Ethazi/gaitasunak/form";
    }
    @PostMapping({"/gaitasunak/berria", "/gaitasunak/{id}/editatu"})
    public String gaitasunGorde(@PathVariable(required = false) Long id, @ModelAttribute("form") GaitasunaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding);
            Long saved = service.gordeGaitasuna(id, form);
            flash.addFlashAttribute("success", "Gaitasuna gorde da.");
            return "redirect:/ethazi/gaitasunak/" + saved + "/editatu";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            return gaitasunForm(id, form, model);
        }
    }
    @PostMapping("/gaitasunak/{id}/ezabatu")
    public String gaitasunEzabatu(@PathVariable Long id, RedirectAttributes flash) {
        var g = service.gaitasuna(id);
        return egin(() -> service.ezabatuGaitasuna(id), "Gaitasuna ezabatu da.",
                g.getMota() == GaitasunMota.ZEHARKAKOA ? "/gaitasunak/zeharkakoak" : "/gaitasunak/teknikoak", flash);
    }
    @PostMapping({"/gaitasunak/{id}/mailak/{mailaId}/adierazleak/berria",
            "/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/editatu"})
    public String adierazleGorde(@PathVariable Long id, @PathVariable Long mailaId,
            @PathVariable(required = false) Long adierazleaId, @ModelAttribute AdierazleaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); service.gordeAdierazlea(id, mailaId, adierazleaId, form);
            flash.addFlashAttribute("success", "Lorpen-adierazlea gorde da.");
            return "redirect:/ethazi/gaitasunak/" + id + "/editatu#adierazleak";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            model.addAttribute("failedForm", form);
            model.addAttribute("failedMailaId", mailaId);
            model.addAttribute("failedAdierazleaId", adierazleaId);
            return gaitasunForm(id, service.gaitasunaForm(id, null, null), model);
        }
    }
    @PostMapping("/gaitasunak/{id}/mailak/{mailaId}/adierazleak/{adierazleaId}/ezabatu")
    public String adierazleEzabatu(@PathVariable Long id, @PathVariable Long mailaId,
            @PathVariable Long adierazleaId, RedirectAttributes flash) {
        return egin(() -> service.ezabatuAdierazlea(id, mailaId, adierazleaId), "Lorpen-adierazlea ezabatu da.",
                "/gaitasunak/" + id + "/editatu#adierazleak", flash);
    }

    @GetMapping("/mailakatzeak")
    public String mailakatzeak(Model model) {
        model.addAttribute("ereduak", service.ereduak());
        return "Ethazi/mailakatzeak/index";
    }
    @GetMapping("/mailakatzeak/berria")
    public String ereduBerria(Model model) { return ereduForm(null, new EreduaForm(), model); }
    @GetMapping("/mailakatzeak/{id}/editatu")
    public String ereduEditatu(@PathVariable Long id, Model model) {
        var e = service.eredua(id); var form = new EreduaForm();
        form.setZikloaId(e.getZikloa().getId()); form.setMota(e.getMota()); form.setIzena(e.getIzena()); form.setIzenaEs(e.getIzenaEs()); form.setIzenaEn(e.getIzenaEn());
        return ereduForm(id, form, model);
    }
    private String ereduForm(Long id, EreduaForm form, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("form", form);
        model.addAttribute("eredua", id == null ? null : service.eredua(id));
        return "Ethazi/mailakatzeak/form";
    }
    @PostMapping({"/mailakatzeak/berria", "/mailakatzeak/{id}/editatu"})
    public String ereduGorde(@PathVariable(required = false) Long id, @ModelAttribute("form") EreduaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); Long saved = service.gordeEredua(id, form);
            flash.addFlashAttribute("success", "Mailakatze eredua gorde da.");
            return "redirect:/ethazi/mailakatzeak/" + saved + "/editatu";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            return ereduForm(id, form, model);
        }
    }
    @PostMapping("/mailakatzeak/{id}/ezabatu")
    public String ereduEzabatu(@PathVariable Long id, RedirectAttributes flash) {
        return egin(() -> service.ezabatuEredua(id), "Mailakatze eredua ezabatu da.", "/mailakatzeak", flash);
    }
    @PostMapping({"/mailakatzeak/{id}/mailak/berria", "/mailakatzeak/{id}/mailak/{mailaId}/editatu"})
    public String mailaGorde(@PathVariable Long id, @PathVariable(required = false) Long mailaId,
            @ModelAttribute MailaForm form, BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); service.gordeMaila(id, mailaId, form);
            flash.addFlashAttribute("success", "Maila gorde da.");
            return "redirect:/ethazi/mailakatzeak/" + id + "/editatu";
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            model.addAttribute("failedMaila", form);
            model.addAttribute("failedMailaId", mailaId);
            return ereduEditatu(id, model);
        }
    }
    @PostMapping("/mailakatzeak/{id}/mailak/{mailaId}/ezabatu")
    public String mailaEzabatu(@PathVariable Long id, @PathVariable Long mailaId, RedirectAttributes flash) {
        return egin(() -> service.ezabatuMaila(id, mailaId), "Maila ezabatu da.", "/mailakatzeak/" + id + "/editatu", flash);
    }
    @PostMapping("/mailakatzeak/{id}/mailak/{mailaId}/mugitu")
    public String mailaMugitu(@PathVariable Long id, @PathVariable Long mailaId,
            @RequestParam int norabidea, RedirectAttributes flash) {
        return egin(() -> service.mugituMaila(id, mailaId, norabidea), "Mailen ordena aldatu da.", "/mailakatzeak/" + id + "/editatu", flash);
    }

    @GetMapping("/ikaskuntza-emaitzak")
    public String emaitzak(@RequestParam(required = false) Long zikloaId, @RequestParam(required = false) Long moduloaId, Model model) {
        var modules = new java.util.ArrayList<>(service.moduluak(zikloaId));
        modules.sort(java.util.Comparator
                .comparing(Moduloa::getKodea, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Moduloa::getIzena, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        model.addAttribute("zikloaId", zikloaId);
        model.addAttribute("moduloaId", moduloaId);
        model.addAttribute("modulua", moduloaId == null || zikloaId == null ? null : service.moduloa(zikloaId, moduloaId));
        model.addAttribute("moduluak", modules);
        model.addAttribute("emaitzaKopuruak", service.emaitzaKopuruak(modules));
        model.addAttribute("emaitzak", service.emaitzak(zikloaId, moduloaId));
        return "Ethazi/ikaskuntza-emaitzak/index";
    }
    @GetMapping("/ikaskuntza-emaitzak/berria")
    public String emaitzaBerria(@RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long moduloaId, Model model) {
        var f = new EmaitzaForm(); f.setZikloaId(zikloaId); f.setModuloaId(moduloaId);
        return emaitzaForm(null, f, model);
    }
    @GetMapping("/ikaskuntza-emaitzak/{id}/editatu")
    public String emaitzaEditatu(@PathVariable Long id, @RequestParam(required=false) Long moduloaId,
            @RequestParam(required=false) Long zikloaId, Model model) {
        var form = service.emaitzaForm(id);
        if (moduloaId != null && zikloaId != null) {
            if (!service.dagokio(service.emaitza(id), service.moduloa(zikloaId, moduloaId)))
                throw new IllegalArgumentException("IE ez da modulu horretakoa.");
            form.setModuloaId(moduloaId); form.setZikloaId(zikloaId);
        }
        return emaitzaForm(id, form, model);
    }
    private String emaitzaForm(Long id, EmaitzaForm form, Model model) {
        var modules = service.moduluak(form.getZikloaId());
        var language = modules.stream().filter(m -> Objects.equals(m.getId(), form.getModuloaId()))
                .map(Moduloa::getHizkuntza).filter(h -> h != Hizkuntza.ZEHAZTU_GABE)
                .findFirst().orElse(Hizkuntza.EUSKARA);
        model.addAttribute("id", id);
        model.addAttribute("form", form);
        model.addAttribute("moduluak", modules);
        model.addAttribute("testuHizkuntza", language);
        return "Ethazi/ikaskuntza-emaitzak/form";
    }
    @PostMapping({"/ikaskuntza-emaitzak/berria", "/ikaskuntza-emaitzak/{id}/editatu"})
    public String emaitzaGorde(@PathVariable(required = false) Long id, @ModelAttribute("form") EmaitzaForm form,
            BindingResult binding, Model model, RedirectAttributes flash) {
        try {
            balidatu(binding); service.gordeEmaitza(id, form);
            flash.addFlashAttribute("success", "Ikaskuntza-emaitza gorde da.");
            return "redirect:/ethazi/ikaskuntza-emaitzak?zikloaId=" + form.getZikloaId() + "&moduloaId=" + form.getModuloaId();
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            model.addAttribute("error", mezua(ex));
            return emaitzaForm(id, form, model);
        }
    }
    @PostMapping("/ikaskuntza-emaitzak/{id}/ezabatu")
    public String emaitzaEzabatu(@PathVariable Long id, RedirectAttributes flash) {
        var f = service.emaitzaForm(id);
        return egin(() -> service.ezabatuEmaitza(id), "Ikaskuntza-emaitza ezabatu da.",
                "/ikaskuntza-emaitzak?zikloaId=" + f.getZikloaId() + "&moduloaId=" + f.getModuloaId(), flash);
    }
    @PostMapping("/ikaskuntza-emaitzak/inportatu")
    public String emaitzakInportatu(@RequestParam("fitxategia") MultipartFile fitxategia,
            @RequestParam(required = false) Long zikloaId,
            @RequestParam(required = false) Long moduloaId, RedirectAttributes flash) {
        try {
            var result = emaitzaCsvImportService.inportatu(fitxategia);
            String mezua = "CSV inportazioa osatu da: " + result.sortuak()
                    + " sortu eta " + result.eguneratuak() + " eguneratu (" + result.guztira() + " guztira).";
            if (!result.kargatuGabe().isEmpty()) {
                mezua += " Kargatu gabe: " + String.join("; ", result.kargatuGabe()) + ".";
            }
            flash.addFlashAttribute("success", mezua);
        } catch (IllegalArgumentException | DataIntegrityViolationException ex) {
            flash.addFlashAttribute("error", mezua(ex));
        }
        String destination = "redirect:/ethazi/ikaskuntza-emaitzak";
        if (zikloaId != null) destination += "?zikloaId=" + zikloaId;
        if (zikloaId != null && moduloaId != null) destination += "&moduloaId=" + moduloaId;
        return destination;
    }
    private void balidatu(BindingResult binding) {
        if (binding.hasErrors()) throw new IllegalArgumentException("Berrikusi formularioa: zenbaki edo aukera baliogabea.");
    }
    private String egin(Runnable action, String success, String destination, RedirectAttributes flash) {
        try { action.run(); flash.addFlashAttribute("success", success); }
        catch (IllegalArgumentException | DataIntegrityViolationException ex) { flash.addFlashAttribute("error", mezua(ex)); }
        return "redirect:/ethazi" + destination;
    }
    private String mezua(Exception ex) {
        return ex instanceof DataIntegrityViolationException
                ? "Ezin da eragiketa burutu: datuak erabiltzen ari dira edo erregistro bikoiztua dago." : ex.getMessage();
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public String ezDaAurkitu(IllegalArgumentException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage()); return "redirect:/ethazi/gaitasunak";
    }
}
