package com.koadernoa.app.ethazi.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface GaitasunaRepository extends JpaRepository<Gaitasuna, Long> {
    List<Gaitasuna> findByZikloaIdAndMotaOrderByKodeaAsc(Long zikloaId, GaitasunMota mota);
    boolean existsByZikloaIdAndMota(Long zikloaId, GaitasunMota mota);
}
