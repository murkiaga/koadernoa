package com.koadernoa.app.objektuak.konfigurazioa.service;

import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.konfigurazioa.entitateak.AplikazioAukera;
import com.koadernoa.app.objektuak.konfigurazioa.repository.AplikazioAukeraRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AplikazioAukeraService {
	
	
	public static final String EBAL1_KOLORE = "ebal1.kolore";
    public static final String EBAL2_KOLORE = "ebal2.kolore";
    public static final String EBAL3_KOLORE = "ebal3.kolore";
    
    public static final String APP_LOGO_URL = "APP_LOGO_URL"; // adib: /uploads/logo.png
    public static final String APP_FAVICON_URL = "APP_FAVICON_URL";
    
    public static final String AUTH_GOOGLE_ENABLED = "auth.google.enabled";
    public static final String AUTH_LDAP_ENABLED = "auth.ldap.enabled";
    public static final String AUTH_DEFAULT = "auth.default";
    public static final String KOADERNO_BESTE_MINTEGIA_BAIMENDU = "koadernoak.beste.mintegia.baimendu";
    public static final String KOADERNO_BIKOIZTUAK_BAIMENDU = "koadernoak.bikoiztuak.baimendu";
    public static final String PLANGINTZA_KONTROL_AUTOMATIKOA = "PLANGINTZA_KONTROL_AUTOMATIKOA";
    public static final String PLANGINTZA_KONTROL_LANEGUNAK = "PLANGINTZA_KONTROL_LANEGUNAK";
    public static final String JOKABIDE_DESEGOKIEN_ARDURADUNA_ID = "jokabide.desegokiak.arduraduna.id";
    public static final int PLANGINTZA_KONTROL_LANEGUNAK_DEFEKTUZ = 10;
    public static final int PLANGINTZA_KONTROL_LANEGUNAK_GEHIENEZ = 365;
    
    private final AplikazioAukeraRepository repo;

    public String get(String gakoa) {
        return repo.findById(gakoa).map(AplikazioAukera::getBalioa).orElse(null);
    }

    public boolean googleDa() {
        return "google".equalsIgnoreCase(get("auth.mota"));
    }

    public boolean ldapDa() {
        return "ldap".equalsIgnoreCase(get("auth.mota"));
    }
    
    public String get(String giltza, String defektuzkoBalioa) {
        return repo.findById(giltza)
                   .map(AplikazioAukera::getBalioa)
                   .orElse(defektuzkoBalioa);
    }
    
    public boolean getBool(String giltza, boolean defektuzkoa) {
        String balioa = get(giltza, Boolean.toString(defektuzkoa));
        return Boolean.parseBoolean(balioa);
    }

    public void set(String giltza, String balioa) {
        AplikazioAukera a = repo.findById(giltza)
                                .orElseGet(() -> {
                                    AplikazioAukera n = new AplikazioAukera();
                                    n.setGiltza(giltza);
                                    return n;
                                });
        a.setBalioa(balioa);
        repo.save(a);
    }
    
    public void setBool(String giltza, boolean balioa) {
        set(giltza, Boolean.toString(balioa));
    }

    public Long getJokabideDesegokienArduradunaId() {
        String balioa = get(JOKABIDE_DESEGOKIEN_ARDURADUNA_ID);
        if (balioa == null || balioa.isBlank()) return null;
        try {
            return Long.valueOf(balioa);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public void setJokabideDesegokienArduradunaId(Long irakasleaId) {
        set(JOKABIDE_DESEGOKIEN_ARDURADUNA_ID, irakasleaId == null ? "" : irakasleaId.toString());
    }

    public boolean isPlangintzaKontrolAutomatikoaAktibo() {
        return getBool(PLANGINTZA_KONTROL_AUTOMATIKOA, false);
    }

    public void setPlangintzaKontrolAutomatikoa(boolean aktibo) {
        setBool(PLANGINTZA_KONTROL_AUTOMATIKOA, aktibo);
    }

    public int getPlangintzaKontrolLanegunak() {
        String balioa = get(PLANGINTZA_KONTROL_LANEGUNAK,
                Integer.toString(PLANGINTZA_KONTROL_LANEGUNAK_DEFEKTUZ));
        try {
            return mugatuPlangintzaKontrolLanegunak(Integer.parseInt(balioa));
        } catch (NumberFormatException ex) {
            return PLANGINTZA_KONTROL_LANEGUNAK_DEFEKTUZ;
        }
    }

    public void setPlangintzaKontrolLanegunak(Integer lanegunak) {
        int balioa = lanegunak == null
                ? PLANGINTZA_KONTROL_LANEGUNAK_DEFEKTUZ
                : mugatuPlangintzaKontrolLanegunak(lanegunak);
        set(PLANGINTZA_KONTROL_LANEGUNAK, Integer.toString(balioa));
    }

    private int mugatuPlangintzaKontrolLanegunak(int lanegunak) {
        return Math.min(PLANGINTZA_KONTROL_LANEGUNAK_GEHIENEZ, Math.max(1, lanegunak));
    }
}
