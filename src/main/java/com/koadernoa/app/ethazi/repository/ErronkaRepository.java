package com.koadernoa.app.ethazi.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.Erronka;
public interface ErronkaRepository extends JpaRepository<Erronka,Long> {
    List<Erronka> findByZikloaIdOrderByHasieraDataDescIdDesc(Long zikloaId);
}
