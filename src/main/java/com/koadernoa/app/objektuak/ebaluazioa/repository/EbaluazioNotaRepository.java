package com.koadernoa.app.objektuak.ebaluazioa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioNota;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.modulua.entitateak.Matrikula;

public interface EbaluazioNotaRepository extends JpaRepository<EbaluazioNota, Long> {

    Optional<EbaluazioNota> findByMatrikulaAndEbaluazioMomentua(Matrikula matrikula,
                                                                EbaluazioMomentua ebaluazioMomentua);

    // Koaderno bereko nota guztiak kargatzeko (hasierako pantailarako)
    List<EbaluazioNota> findByMatrikulaKoadernoa(Koadernoa koadernoa);
    
    boolean existsByEgoera(EbaluazioEgoera egoera);
    
    void deleteByMatrikula_Koadernoa_Id(Long koadernoId);
}
