package com.koadernoa.app.objektuak.ordutegiak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.koadernoa.app.objektuak.ordutegiak.entitateak.TaldeOrdutegia;

public interface TaldeOrdutegiaRepository extends JpaRepository<TaldeOrdutegia, Long> {
    @EntityGraph(attributePaths = {"taldea", "ikasturtea", "lerroak", "lerroak.moduloa"})
    List<TaldeOrdutegia> findByTaldea_IdAndIkasturtea_IdOrderByHasieraDataAsc(Long taldeaId, Long ikasturteaId);

    @EntityGraph(attributePaths = {"taldea", "ikasturtea", "lerroak", "lerroak.moduloa"})
    @Query("select o from TaldeOrdutegia o where o.id = :id")
    Optional<TaldeOrdutegia> findWithLerroakById(Long id);
}
