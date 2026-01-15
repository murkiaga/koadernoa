package com.koadernoa.app.objektuak.koadernoak.service;

import java.util.Collections;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.koadernoa.app.funtzionalitateak.kudeatzaile.EstatistikaDashboard.EstatistikakFiltroa;
import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.repository.EstatistikaEbaluazioanRepository;
import com.koadernoa.app.objektuak.koadernoak.repository.projection.EbaluazioKodeKopuruaProjection;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EstatistikakKudeatzaileService {
	private final EstatistikaEbaluazioanRepository estatistikaRepo;
	
	private String nullIfBlank(String s) {
	    return (s == null || s.isBlank()) ? null : s;
	}
	
	public List<EbaluazioKodeKopuruaProjection> beteGabeakEbaluazioKodez(EstatistikakFiltroa f) {
	    return estatistikaRepo.countKalkulatuGabeakKodezAktiboan(
	        f.getFamiliaId(), f.getZikloaId(), f.getTaldeaId(), f.getMailaId()
	    );
	  }

	public Page<EstatistikaEbaluazioan> bilatuOrrikatuta(EstatistikakFiltroa f, Pageable pageable) {
	    return estatistikaRepo.bilatuDashboarderako(
	        nullIfBlank(f.getEbaluazioKodea()),
	        f.getKalkulatua(),
	        f.getFamiliaId(),
	        f.getZikloaId(),
	        f.getTaldeaId(),
	        f.getMailaId(),
	        pageable
	    );
	}


    // ---- Dropdown datuak: hemen zure zerbitzuetara konektatu (FamiliaService, ZikloaService, TaldeaService, MailaService...)
	public List<Familia> lortuFamiliak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findFamiliaAktiboak();
	}
	public List<Zikloa> lortuZikloak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findZikloAktiboak(f.getFamiliaId());
	}
	public List<Taldea> lortuTaldeak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findTaldeAktiboak(f.getZikloaId());
	}
	public List<Maila> lortuMailak(EstatistikakFiltroa f) {
	  return estatistikaRepo.findMailaAktiboak();
	}
    public List<String> lortuEbaluazioKodeak(EstatistikakFiltroa f) { return List.of("1_EBAL","2_EBAL","3_EBAL","1_FINAL","2_FINAL"); }
}
