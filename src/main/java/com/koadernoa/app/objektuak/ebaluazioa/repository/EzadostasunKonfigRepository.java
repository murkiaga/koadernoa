package com.koadernoa.app.objektuak.ebaluazioa.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.ebaluazioa.entitateak.EzadostasunKonfig;

public interface EzadostasunKonfigRepository extends JpaRepository<EzadostasunKonfig, Long> {

	Optional<EzadostasunKonfig> findByKodea(String kodea);

    List<EzadostasunKonfig> findAllByOrderByKodeaAsc();
    
    boolean existsByKodeaIgnoreCase(String kodea);
}
