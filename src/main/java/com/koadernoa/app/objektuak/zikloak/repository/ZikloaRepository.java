package com.koadernoa.app.objektuak.zikloak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

public interface ZikloaRepository extends JpaRepository<Zikloa, Long> {
    Optional<Zikloa> findByIzena(String izena);
    Optional<Zikloa> findByIzenaIgnoreCaseAndFamilia(String izena, Familia familia);
}
