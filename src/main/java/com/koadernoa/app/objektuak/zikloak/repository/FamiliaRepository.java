package com.koadernoa.app.objektuak.zikloak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;

public interface FamiliaRepository extends JpaRepository<Familia, Long> {

	boolean existsByIzenaIgnoreCase(String izena);
    Optional<Familia> findByIzenaIgnoreCase(String izena);
    boolean existsBySlugIgnoreCase(String slug);
    Optional<Familia> findBySlugIgnoreCase(String slug);
    List<Familia> findAllByAktiboTrueOrderByIzenaAsc();
}
