package com.koadernoa.app.objektuak.koadernoak.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.koadernoa.app.objektuak.egutegia.entitateak.Maila;
import com.koadernoa.app.objektuak.koadernoak.entitateak.EstatistikaEbaluazioan;
import com.koadernoa.app.objektuak.koadernoak.repository.projection.EbaluazioKodeKopuruaProjection;
import com.koadernoa.app.objektuak.zikloak.entitateak.Familia;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.zikloak.entitateak.Zikloa;

public interface EstatistikaEbaluazioanRepository extends JpaRepository<EstatistikaEbaluazioan, Long>, JpaSpecificationExecutor<EstatistikaEbaluazioan> {

	// -------------------------------------------------------------------------
    // Irakasle atala
    // -------------------------------------------------------------------------

    List<EstatistikaEbaluazioan> findByKoadernoaIdOrderByEbaluazioMomentua_OrdenaAscIdAsc(Long koadernoId);

    void deleteByKoadernoa_Id(Long koadernoId);

    // -------------------------------------------------------------------------
    // Debug / Diagnostikoa
    // -------------------------------------------------------------------------

    @Query("select count(es) from EstatistikaEbaluazioan es")
    long countGuztira();

    @Query("""
        select count(es)
        from EstatistikaEbaluazioan es
          join es.koadernoa k
          join k.egutegia e
          join e.ikasturtea ik
        where ik.aktiboa = true
    """)
    long countIkasturteAktiboan();

    // -------------------------------------------------------------------------
    // Zerrenda orrikatua (Dashboard)
    // -------------------------------------------------------------------------

    @EntityGraph(attributePaths = {
    	    "koadernoa",
    	    "koadernoa.irakasleak",
    	    "koadernoa.moduloa",
    	    "koadernoa.moduloa.taldea",
    	    "koadernoa.egutegia",
    	    "koadernoa.egutegia.maila",
    	    "ebaluazioMomentua"
    	})
    @Query(
        value = """
            select es
            from EstatistikaEbaluazioan es
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
            select count(es)
            from EstatistikaEbaluazioan es
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
    Page<EstatistikaEbaluazioan> bilatuDashboarderako(
        @Param("ebaluazioKodea") String ebaluazioKodea,
        @Param("kalkulatua") Boolean kalkulatua,
        @Param("familiaId") Long familiaId,
        @Param("zikloaId") Long zikloaId,
        @Param("taldeaId") Long taldeaId,
        @Param("mailaId") Long mailaId,
        Pageable pageable
    );

    // -------------------------------------------------------------------------
    // Txartelak: ebaluazio-kode bakoitzeko kalkulatu gabe daudenak
    // -------------------------------------------------------------------------

    @Query("""
        select em.kodea as kodea, count(es) as kopurua
        from EstatistikaEbaluazioan es
          join es.ebaluazioMomentua em
          join es.koadernoa k
          join k.egutegia e
          join e.ikasturtea ik
          join k.moduloa m
          join m.taldea t
          join t.zikloa z
          join z.familia f
        where ik.aktiboa = true
          and es.kalkulatua = false
          and (:familiaId is null or f.id = :familiaId)
          and (:zikloaId is null or z.id = :zikloaId)
          and (:taldeaId is null or t.id = :taldeaId)
          and (:mailaId is null or e.maila.id = :mailaId)
        group by em.kodea, em.ordena
        order by em.ordena asc
    """)
    List<EbaluazioKodeKopuruaProjection> countKalkulatuGabeakKodezAktiboan(
        @Param("familiaId") Long familiaId,
        @Param("zikloaId") Long zikloaId,
        @Param("taldeaId") Long taldeaId,
        @Param("mailaId") Long mailaId
    );

    // -------------------------------------------------------------------------
    // Totala: count orokorra filtroekin (ikasturte aktiboan)
    // -------------------------------------------------------------------------

    

    // -------------------------------------------------------------------------
    // SELECT-ak betetzeko (dropdown-ak)
    // -------------------------------------------------------------------------

    @Query("""
        select distinct f
        from EstatistikaEbaluazioan es
          join es.koadernoa k
          join k.egutegia e
          join e.ikasturtea ik
          join k.moduloa m
          join m.taldea t
          join t.zikloa z
          join z.familia f
        where ik.aktiboa = true
        order by f.izena
    """)
    List<Familia> findFamiliaAktiboak();

    @Query("""
        select distinct z
        from EstatistikaEbaluazioan es
          join es.koadernoa k
          join k.egutegia e
          join e.ikasturtea ik
          join k.moduloa m
          join m.taldea t
          join t.zikloa z
          join z.familia f
        where ik.aktiboa = true
          and (:familiaId is null or f.id = :familiaId)
        order by z.izena
    """)
    List<Zikloa> findZikloAktiboak(@Param("familiaId") Long familiaId);

    @Query("""
        select distinct t
        from EstatistikaEbaluazioan es
          join es.koadernoa k
          join k.egutegia e
          join e.ikasturtea ik
          join k.moduloa m
          join m.taldea t
          join t.zikloa z
        where ik.aktiboa = true
          and (:zikloaId is null or z.id = :zikloaId)
        order by t.izena
    """)
    List<Taldea> findTaldeAktiboak(@Param("zikloaId") Long zikloaId);

    @Query("""
	  select distinct maila
	  from EstatistikaEbaluazioan es
	    join es.koadernoa k
	    join k.egutegia e
	    join e.ikasturtea ik
	    join e.maila maila
	  where ik.aktiboa = true
	  order by maila.ordena, maila.id
	""")
	List<Maila> findMailaAktiboak();
}
