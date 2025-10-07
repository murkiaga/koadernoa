package com.koadernoa.app.objektuak.konfigurazioa.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.koadernoa.app.objektuak.konfigurazioa.entitateak.AplikazioAukera;
import com.koadernoa.app.objektuak.konfigurazioa.repository.AplikazioAukeraRepository;

@Service
public class AplikazioAukeraService {
	@Autowired
    private AplikazioAukeraRepository repo;

    public String get(String gakoa) {
        return repo.findById(gakoa).map(AplikazioAukera::getBalioa).orElse(null);
    }

    public boolean googleDa() {
        return "google".equalsIgnoreCase(get("auth.mota"));
    }

    public boolean ldapDa() {
        return "ldap".equalsIgnoreCase(get("auth.mota"));
    }
}
