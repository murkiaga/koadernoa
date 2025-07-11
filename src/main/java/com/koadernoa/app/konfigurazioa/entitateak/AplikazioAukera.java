package com.koadernoa.app.konfigurazioa.entitateak;

import java.util.List;

import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.zikloak.entitateak.Taldea;

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
