package com.koadernoa.app.objektuak.ebaluazioa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EbaluazioEgoera;
import com.koadernoa.app.objektuak.ebaluazioa.entitateak.PendienteEbaluazioMomentuKonfig;

public interface PendienteEbaluazioMomentuKonfigRepository
        extends JpaRepository<PendienteEbaluazioMomentuKonfig, Long> {

    List<PendienteEbaluazioMomentuKonfig> findAllByOrderByKodeaAsc();

    Optional<PendienteEbaluazioMomentuKonfig> findByKodeaIgnoreCase(String kodea);

    boolean existsByEgoeraOnartuakContains(EbaluazioEgoera egoera);
}
