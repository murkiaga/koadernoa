package com.koadernoa.app.objektuak.koadernoak.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.NotaFitxategia;

public interface NotaFitxategiaRepository extends JpaRepository<NotaFitxategia, Long> {

    void deleteByKoadernoa_Id(Long koadernoId);

}
