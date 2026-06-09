package com.koadernoa.app.objektuak.jokabidea.repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.JokabideDesegokia;
public interface JokabideDesegokiaRepository extends JpaRepository<JokabideDesegokia, Long> {
    List<JokabideDesegokia> findByKoadernoaIdAndDataBetween(Long koadernoaId, LocalDate hasiera, LocalDate amaiera);
    List<JokabideDesegokia> findByKoadernoaIdAndData(Long koadernoaId, LocalDate data);
    List<JokabideDesegokia> findByIkasleaIdAndKoadernoaIdAndDataOrderByCreatedAtDesc(Long ikasleaId, Long koadernoaId, LocalDate data);
}
