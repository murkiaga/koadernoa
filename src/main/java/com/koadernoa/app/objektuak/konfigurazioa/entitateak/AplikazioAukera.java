package com.koadernoa.app.objektuak.konfigurazioa.entitateak;

import java.util.List;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class AplikazioAukera {

    @Id
    private String giltza;

    private String balioa;

    //Adibidez: giltza = "ikasturteAktiboa", balioa = "2025-2026"
}
