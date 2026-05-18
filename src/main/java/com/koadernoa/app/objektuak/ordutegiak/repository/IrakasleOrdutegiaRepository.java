package com.koadernoa.app.objektuak.ordutegiak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;

public interface IrakasleOrdutegiaRepository extends JpaRepository<IrakasleOrdutegia, Long> {
    void deleteByIkasturteaId(Long ikasturteaId);

    @EntityGraph(attributePaths = {"lerroak", "lerroak.taldea"})
    Optional<IrakasleOrdutegia> findByIrakasleaIdAndIkasturteaId(Long irakasleaId, Long ikasturteaId);
}
