package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.Programazioa;

public interface ProgramazioaRepository extends JpaRepository<Programazioa, Long> {
    Optional<Programazioa> findByKoadernoa(Koadernoa koadernoa);
}
