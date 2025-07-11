package com.koadernoa.app.zikloak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.zikloak.entitateak.Zikloa;

public interface ZikloaRepository extends JpaRepository<Zikloa, Long> {
    Optional<Zikloa> findByIzena(String izena);
}
