package com.koadernoa.app.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.koadernoak.entitateak.Koadernoa;
import com.koadernoa.app.koadernoak.entitateak.Ordutegia;

public interface OrdutegiaRepository extends JpaRepository<Ordutegia, Long> {
    List<Ordutegia> findByKoadernoaOrderByEgunaAscOrduHasieraAsc(Koadernoa koadernoa);
}