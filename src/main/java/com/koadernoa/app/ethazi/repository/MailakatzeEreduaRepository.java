package com.koadernoa.app.ethazi.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.ethazi.entitateak.gaitasunak.*;

public interface MailakatzeEreduaRepository extends JpaRepository<MailakatzeEredua, Long> {
    Optional<MailakatzeEredua> findByZikloaIdAndMota(Long zikloaId, GaitasunMota mota);
}
