package com.koadernoa.app.zikloak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.irakasleak.entitateak.Irakaslea;
import com.koadernoa.app.zikloak.entitateak.Taldea;

public interface TaldeaRepository extends JpaRepository<Taldea, Long> {
	Optional<Taldea> findByTutorea(Irakaslea tutorea);
    Optional<Taldea> findByTutorea_Id(Long irakasleId);
    boolean existsByTutorea_Id(Long irakasleId);
    
    //zikloaren arabera taldeak
    List<Taldea> findByZikloa_Id(Long zikloaId);
}