package com.koadernoa.app.ethazi.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import com.koadernoa.app.ethazi.entitateak.Erronka;
public interface ErronkaRepository extends JpaRepository<Erronka,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from Erronka e where e.id = :id")
    java.util.Optional<Erronka> findLockedById(Long id);
    List<Erronka> findByBertsioTaldea(Long bertsioTaldea);
    List<Erronka> findByZikloaIdOrderByHasieraDataDescIdDesc(Long zikloaId);
    List<Erronka> findByZikloaIdAndIkasturteaIdOrderByHasieraDataDescIdDesc(Long zikloaId, Long ikasturteaId);

    @EntityGraph(attributePaths = "moduluak")
    @Query("select distinct e from Erronka e where e.zikloa.id = :zikloaId and e.ikasturtea.id = :ikasturteaId")
    List<Erronka> findWithModuluakByZikloaIdAndIkasturteaId(Long zikloaId, Long ikasturteaId);
}
