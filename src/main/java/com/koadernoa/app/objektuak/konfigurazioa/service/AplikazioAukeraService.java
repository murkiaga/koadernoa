package com.koadernoa.app.objektuak.konfigurazioa.service;

import org.springframework.beans.factory.annotation.Autowired;
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
}
