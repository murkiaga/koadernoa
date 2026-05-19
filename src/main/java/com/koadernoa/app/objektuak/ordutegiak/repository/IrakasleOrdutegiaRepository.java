package com.koadernoa.app.objektuak.ordutegiak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;

public interface IrakasleOrdutegiaRepository extends JpaRepository<IrakasleOrdutegia, Long> {
    void deleteByIkasturteaId(Long ikasturteaId);

    @EntityGraph(attributePaths = {"lerroak", "lerroak.taldea"})
    Optional<IrakasleOrdutegia> findByIrakasleaIdAndIkasturteaId(Long irakasleaId, Long ikasturteaId);

    @EntityGraph(attributePaths = {"lerroak", "lerroak.taldea"})
    @Query("""
            select io from IrakasleOrdutegia io
            where io.irakaslea.id = :irakasleaId
              and io.ikasturtea.aktiboa = true
            """)
    Optional<IrakasleOrdutegia> findAktiboenaByIrakasleaId(Long irakasleaId);
}
