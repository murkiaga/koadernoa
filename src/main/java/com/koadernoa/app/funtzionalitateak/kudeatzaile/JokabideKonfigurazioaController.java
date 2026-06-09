package com.koadernoa.app.funtzionalitateak.kudeatzaile;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.koadernoa.app.objektuak.jokabidea.entitateak.*;
import com.koadernoa.app.objektuak.jokabidea.repository.*;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/kudeatzaile/konfigurazioa/jokabide-desegokia")
@RequiredArgsConstructor
public class JokabideKonfigurazioaController {
    private final PortaeraArrazoiaRepository arrazoiak;
    private final NeurriZuzentzaileaRepository neurriak;

    @PostMapping("/portaera-arrazoiak/sortu") @Transactional
    public String sortuArrazoia(@RequestParam String kodea,@RequestParam String testua,@RequestParam(defaultValue="0") int ordena,@RequestParam(defaultValue="false") boolean defektuzkoa,RedirectAttributes ra){
        if(hutsa(kodea)||hutsa(testua)){ra.addFlashAttribute("error","Kodea eta testua beharrezkoak dira.");return redirect();}
        PortaeraArrazoia a=new PortaeraArrazoia();a.setKodea(kodea.trim());a.setTestua(testua.trim());a.setOrdena(ordena);a.setAktibo(true);a.setDefektuzkoa(defektuzkoa);if(defektuzkoa)kenduArrazoiDefektuzkoak();arrazoiak.save(a);ra.addFlashAttribute("success","Portaera arrazoia sortu da.");return redirect();}
    @PostMapping("/portaera-arrazoiak/{id}/gorde") @Transactional
    public String gordeArrazoia(@PathVariable Long id,@RequestParam String kodea,@RequestParam String testua,@RequestParam int ordena,@RequestParam(defaultValue="false") boolean aktibo,@RequestParam(defaultValue="false") boolean defektuzkoa,RedirectAttributes ra){
        var a=arrazoiak.findById(id).orElseThrow(()->new IllegalArgumentException("Portaera arrazoia ez da aurkitu."));if(hutsa(kodea)||hutsa(testua)){ra.addFlashAttribute("error","Kodea eta testua beharrezkoak dira.");return redirect();}if(defektuzkoa)kenduArrazoiDefektuzkoak();a.setKodea(kodea.trim());a.setTestua(testua.trim());a.setOrdena(ordena);a.setAktibo(aktibo);a.setDefektuzkoa(defektuzkoa);arrazoiak.save(a);ra.addFlashAttribute("success","Portaera arrazoia eguneratu da.");return redirect();}
    @PostMapping("/portaera-arrazoiak/{id}/ezabatu") @Transactional public String ezabatuArrazoia(@PathVariable Long id){var a=arrazoiak.findById(id).orElseThrow();a.setAktibo(false);a.setDefektuzkoa(false);arrazoiak.save(a);return redirect();}

    @PostMapping("/neurri-zuzentzaileak/sortu") @Transactional
    public String sortuNeurria(@RequestParam String kodea,@RequestParam String testua,@RequestParam(defaultValue="0") int ordena,@RequestParam(defaultValue="false") boolean defektuzkoa,RedirectAttributes ra){
        if(hutsa(kodea)||hutsa(testua)){ra.addFlashAttribute("error","Kodea eta testua beharrezkoak dira.");return redirect();}NeurriZuzentzailea n=new NeurriZuzentzailea();n.setKodea(kodea.trim());n.setTestua(testua.trim());n.setOrdena(ordena);n.setAktibo(true);n.setDefektuzkoa(defektuzkoa);if(defektuzkoa)kenduNeurriDefektuzkoak();neurriak.save(n);ra.addFlashAttribute("success","Neurri zuzentzailea sortu da.");return redirect();}
    @PostMapping("/neurri-zuzentzaileak/{id}/gorde") @Transactional
    public String gordeNeurria(@PathVariable Long id,@RequestParam String kodea,@RequestParam String testua,@RequestParam int ordena,@RequestParam(defaultValue="false") boolean aktibo,@RequestParam(defaultValue="false") boolean defektuzkoa,RedirectAttributes ra){
        var n=neurriak.findById(id).orElseThrow(()->new IllegalArgumentException("Neurri zuzentzailea ez da aurkitu."));if(hutsa(kodea)||hutsa(testua)){ra.addFlashAttribute("error","Kodea eta testua beharrezkoak dira.");return redirect();}if(defektuzkoa)kenduNeurriDefektuzkoak();n.setKodea(kodea.trim());n.setTestua(testua.trim());n.setOrdena(ordena);n.setAktibo(aktibo);n.setDefektuzkoa(defektuzkoa);neurriak.save(n);ra.addFlashAttribute("success","Neurri zuzentzailea eguneratu da.");return redirect();}
    @PostMapping("/neurri-zuzentzaileak/{id}/ezabatu") @Transactional public String ezabatuNeurria(@PathVariable Long id){var n=neurriak.findById(id).orElseThrow();n.setAktibo(false);n.setDefektuzkoa(false);neurriak.save(n);return redirect();}
    private void kenduArrazoiDefektuzkoak(){List<PortaeraArrazoia> l=arrazoiak.findAll();l.forEach(a->a.setDefektuzkoa(false));arrazoiak.saveAll(l);}
    private void kenduNeurriDefektuzkoak(){List<NeurriZuzentzailea> l=neurriak.findAll();l.forEach(n->n.setDefektuzkoa(false));neurriak.saveAll(l);}
    private boolean hutsa(String s){return s==null||s.isBlank();} private String redirect(){return "redirect:/kudeatzaile/konfigurazioa#jokabide-desegokia";}
}
