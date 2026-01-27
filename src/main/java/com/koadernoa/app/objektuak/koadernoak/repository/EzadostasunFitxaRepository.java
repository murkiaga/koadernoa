package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunFitxa;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EzadostasunMota;

public interface EzadostasunFitxaRepository extends JpaRepository<EzadostasunFitxa, Long> {
    Optional<EzadostasunFitxa> findByEstatistikaIdAndMota(Long estatistikaId, EzadostasunMota mota);
    Optional<EzadostasunFitxa> findFirstByEstatistikaIdOrderById(Long estatistikaId);
    List<EzadostasunFitxa> findAllByEstatistikaId(Long estatistikaId);

    @EntityGraph(attributePaths = {
        "estatistika",
        "estatistika.ebaluazioMomentua",
        "estatistika.koadernoa",
        "estatistika.koadernoa.moduloa",
        "estatistika.koadernoa.moduloa.taldea",
        "estatistika.koadernoa.egutegia",
        "estatistika.koadernoa.egutegia.maila"
    })
    @Query(
        value = """
            select ef
            from EzadostasunFitxa ef
              join ef.estatistika es
              join es.ebaluazioMomentua em
              join es.koadernoa k
              join k.egutegia e
              join e.ikasturtea ik
              join k.moduloa m
              join m.taldea t
              join t.zikloa z
              join z.familia f
            where ik.aktiboa = true
              and (:ebaluazioKodea is null or em.kodea = :ebaluazioKodea)
              and (:kalkulatua is null or es.kalkulatua = :kalkulatua)
              and (:familiaId is null or f.id = :familiaId)
              and (:zikloaId is null or z.id = :zikloaId)
              and (:taldeaId is null or t.id = :taldeaId)
              and (:mailaId is null or e.maila.id = :mailaId)
        """,
        countQuery = """
            select count(ef)
            from EzadostasunFitxa ef
              join ef.estatistika es
              join es.ebaluazioMomentua em
              join es.koadernoa k
              join k.egutegia e
              join e.ikasturtea ik
              join k.moduloa m
              join m.taldea t
              join t.zikloa z
              join z.familia f
            where ik.aktiboa = true
              and (:ebaluazioKodea is null or em.kodea = :ebaluazioKodea)
              and (:kalkulatua is null or es.kalkulatua = :kalkulatua)
              and (:familiaId is null or f.id = :familiaId)
              and (:zikloaId is null or z.id = :zikloaId)
              and (:taldeaId is null or t.id = :taldeaId)
              and (:mailaId is null or e.maila.id = :mailaId)
        """
    )
    Page<EzadostasunFitxa> bilatuDashboarderako(
        @Param("ebaluazioKodea") String ebaluazioKodea,
        @Param("kalkulatua") Boolean kalkulatua,
        @Param("familiaId") Long familiaId,
        @Param("zikloaId") Long zikloaId,
        @Param("taldeaId") Long taldeaId,
        @Param("mailaId") Long mailaId,
        Pageable pageable
    );
}
