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
    
    public static final String AUTH_GOOGLE_ENABLED = "auth.google.enabled";
    public static final String AUTH_AD_ENABLED = "auth.ad.enabled";
    public static final String AUTH_LDAP_ENABLED = "auth.ldap.enabled";
    public static final String AUTH_DEFAULT = "auth.default";
    
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
}
