package com.koadernoa.app.ethazi.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.ethazi.entitateak.EthaziDenboralizazioJarduera;

public interface EthaziDenboralizazioJardueraRepository
        extends JpaRepository<EthaziDenboralizazioJarduera, Long> {
    List<EthaziDenboralizazioJarduera> findByEthaziKoadernoaIdAndDataBetweenOrderByDataAscIdAsc(
            Long ethaziKoadernoaId, LocalDate hasiera, LocalDate bukaera);

    @Modifying(flushAutomatically = true)
    @Query("delete from EthaziDenboralizazioJarduera j "
            + "where j.ethaziKoadernoa.id = :koadernoaId and j.erronka.id = :erronkaId")
    void deleteByEthaziKoadernoaIdAndErronkaId(@Param("koadernoaId") Long ethaziKoadernoaId,
            @Param("erronkaId") Long erronkaId);
}
