package com.koadernoa.app.ethazi.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.ethazi.entitateak.EthaziJarraipenGelaxka;

public interface EthaziJarraipenGelaxkaRepository extends JpaRepository<EthaziJarraipenGelaxka, Long> {
    @EntityGraph(attributePaths = {"ikaslea", "moduloa", "erronka"})
    List<EthaziJarraipenGelaxka> findByEthaziKoadernoaIdAndErronkaIdIn(
            Long ethaziKoadernoaId, List<Long> erronkaIds);

    @EntityGraph(attributePaths = {"ikaslea", "moduloa", "erronka"})
    List<EthaziJarraipenGelaxka> findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndData(
            Long ethaziKoadernoaId, Long erronkaId, Long ikasleaId, LocalDate data);

    Optional<EthaziJarraipenGelaxka> findByEthaziKoadernoaIdAndErronkaIdAndIkasleaIdAndModuloaIdAndData(
            Long ethaziKoadernoaId, Long erronkaId, Long ikasleaId, Long moduloaId, LocalDate data);
}
