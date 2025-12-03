package com.koadernoa.app.objektuak.ebaluazioa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;

public interface EbaluazioEgoeraRepository extends JpaRepository<EbaluazioEgoera, Long> {

    /**
     * Zerrenda osoa bueltatzen du, kodea alfabetikoki ordenatuta.
     * Konfigurazio pantailan checkbox zerrenda erakusteko erabilgarria.
     */
    List<EbaluazioEgoera> findAllByOrderByKodeaAsc();

    /**
     * Kode zehatz baten arabera egoera bilatzeko.
     * Adibidez inicializazioan "EZ_AURKEZTUA" dagoen ikusteko.
     */
    Optional<EbaluazioEgoera> findByKodea(String kodea);

    boolean existsByKodea(String kodea);
}
