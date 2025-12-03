package com.koadernoa.app.objektuak.ebaluazioa.repository;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioMomentua;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;


public interface EbaluazioMomentuaRepository extends JpaRepository<EbaluazioMomentua, Long> {

    List<EbaluazioMomentua> findByMailaAndAktiboTrueOrderByOrdenaAsc(Maila maila);
    
    boolean existsByEgoeraOnartuakContains(EbaluazioEgoera egoera);
}
