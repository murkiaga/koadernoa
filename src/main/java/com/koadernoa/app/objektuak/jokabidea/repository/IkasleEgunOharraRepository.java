package com.koadernoa.app.objektuak.jokabidea.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.koadernoa.app.objektuak.jokabidea.entitateak.IkasleEgunOharra;
public interface IkasleEgunOharraRepository extends JpaRepository<IkasleEgunOharra, Long> {
    Optional<IkasleEgunOharra> findByIkasleaIdAndKoadernoaIdAndData(Long ikasleaId, Long koadernoaId, LocalDate data);
    List<IkasleEgunOharra> findByKoadernoaIdAndDataBetween(Long koadernoaId, LocalDate hasiera, LocalDate amaiera);
    List<IkasleEgunOharra> findByKoadernoaIdAndData(Long koadernoaId, LocalDate data);
}
