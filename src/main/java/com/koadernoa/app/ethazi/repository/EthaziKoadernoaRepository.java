package com.koadernoa.app.ethazi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.koadernoa.app.ethazi.entitateak.EthaziKoadernoa;

public interface EthaziKoadernoaRepository extends JpaRepository<EthaziKoadernoa, Long> {
    @EntityGraph(attributePaths = {"taldea", "taldea.zikloa"})
    Optional<EthaziKoadernoa> findByTaldeaId(Long taldeaId);

    @EntityGraph(attributePaths = {"taldea", "taldea.zikloa"})
    @Query("select ek from EthaziKoadernoa ek where ek.id = :id")
    Optional<EthaziKoadernoa> findWithTaldeaById(Long id);

    @EntityGraph(attributePaths = {"taldea", "taldea.zikloa"})
    List<EthaziKoadernoa> findByTaldeaIdInOrderByTaldeaIzenaAsc(List<Long> taldeIds);
}
