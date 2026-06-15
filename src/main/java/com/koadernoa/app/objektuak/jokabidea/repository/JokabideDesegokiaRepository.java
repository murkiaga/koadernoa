package com.koadernoa.app.objektuak.jokabidea.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.koadernoa.app.objektuak.modulua.entitateak.Moduloa;
import com.koadernoa.app.objektuak.zikloak.entitateak.Taldea;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
public interface JokabideDesegokiaRepository extends JpaRepository<JokabideDesegokia, Long> {
    List<JokabideDesegokia> findByKoadernoaIdAndDataBetween(Long koadernoaId, LocalDate hasiera, LocalDate amaiera);
    List<JokabideDesegokia> findByKoadernoaIdAndData(Long koadernoaId, LocalDate data);
    List<JokabideDesegokia> findByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(Long ikasleaId, Long koadernoaId, LocalDate data);
    Optional<JokabideDesegokia> findFirstByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(Long ikasleaId, Long koadernoaId, LocalDate data);

    @Query("""
        select j from JokabideDesegokia j
        join fetch j.ikaslea i
        join fetch j.moduloa m
        left join fetch m.taldea t
        join fetch j.irakaslea ir
        left join fetch j.jasotaNork jn
        join fetch j.portaeraArrazoia pa
        join fetch j.neurriZuzentzailea nz
        where (:dataHasiera is null or j.data >= :dataHasiera)
          and (:dataAmaiera is null or j.data <= :dataAmaiera)
          and (:ikaslea is null or :ikaslea = '' or
               lower(concat(coalesce(i.abizena1, ''), ' ', coalesce(i.abizena2, ''), ' ', coalesce(i.izena, '')))
               like lower(concat('%', :ikaslea, '%')))
          and (:moduloaId is null or m.id = :moduloaId)
          and (:taldeaId is null or t.id = :taldeaId)
          and (:jasota is null or j.jasota = :jasota)
        order by j.createdAt desc, j.id desc
    """)
    List<JokabideDesegokia> bilatuKudeatzailearentzat(
        @Param("dataHasiera") LocalDate dataHasiera,
        @Param("dataAmaiera") LocalDate dataAmaiera,
        @Param("ikaslea") String ikaslea,
        @Param("moduloaId") Long moduloaId,
        @Param("taldeaId") Long taldeaId,
        @Param("jasota") Boolean jasota);

    @Query("""
        select distinct m from JokabideDesegokia j
        join j.moduloa m
        order by m.izena asc
    """)
    List<Moduloa> findDistinctModuloakOrderByIzena();

    @Query("""
        select distinct t from JokabideDesegokia j
        join j.moduloa m
        join m.taldea t
        order by t.izena asc
    """)
    List<Taldea> findDistinctTaldeakOrderByIzena();
}
