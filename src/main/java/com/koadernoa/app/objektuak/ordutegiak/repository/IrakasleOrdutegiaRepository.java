package com.koadernoa.app.objektuak.ordutegiak.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegia;

public interface IrakasleOrdutegiaRepository extends JpaRepository<IrakasleOrdutegia, Long> {
    void deleteByIrakasleaId(Long irakasleaId);
    void deleteByIkasturteaId(Long ikasturteaId);

    @EntityGraph(attributePaths = {"lerroak", "lerroak.taldea", "ikasturtea"})
    List<IrakasleOrdutegia> findAllByIrakasleaId(Long irakasleaId);

    @EntityGraph(attributePaths = {"lerroak", "lerroak.taldea"})
    Optional<IrakasleOrdutegia> findByIrakasleaIdAndIkasturteaId(Long irakasleaId, Long ikasturteaId);

    @EntityGraph(attributePaths = {"lerroak", "lerroak.taldea"})
    @Query("""
            select io from IrakasleOrdutegia io
            where io.irakaslea.id = :irakasleaId
              and io.ikasturtea.aktiboa = true
            """)
    Optional<IrakasleOrdutegia> findAktiboenaByIrakasleaId(Long irakasleaId);

    @EntityGraph(attributePaths = {"irakaslea", "lerroak", "lerroak.taldea"})
    @Query("select distinct io from IrakasleOrdutegia io where io.ikasturtea.aktiboa = true")
    List<IrakasleOrdutegia> findAllAktiboak();

    @EntityGraph(attributePaths = {"irakaslea", "lerroak", "lerroak.taldea"})
    @Query("""
            select distinct io from IrakasleOrdutegia io
            where io.ikasturtea.aktiboa = true and io.irakaslea.id in :irakasleaIds
            """)
    List<IrakasleOrdutegia> findAllAktiboakByIrakasleaIds(@Param("irakasleaIds") List<Long> irakasleaIds);

    @EntityGraph(attributePaths = {"irakaslea", "lerroak", "lerroak.taldea"})
    @Query("""
            select distinct io from IrakasleOrdutegia io
            where io.ikasturtea.id = :ikasturteaId and io.irakaslea.id in :irakasleaIds
            """)
    List<IrakasleOrdutegia> findAllByIkasturteaIdAndIrakasleaIds(
            @Param("ikasturteaId") Long ikasturteaId,
            @Param("irakasleaIds") List<Long> irakasleaIds);
}
