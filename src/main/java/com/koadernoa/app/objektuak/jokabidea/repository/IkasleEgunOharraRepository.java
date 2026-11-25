package com.koadernoa.app.objektuak.jokabidea.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.koadernoa.app.objektuak.jokabidea.entitateak.IkasleEgunOharra;
public interface IkasleEgunOharraRepository extends JpaRepository<IkasleEgunOharra, Long> {
    boolean existsByIrakaslea_Id(Long irakasleId);

    Optional<IkasleEgunOharra> findByIkasleaIdAndKoadernoaIdAndData(Long ikasleaId, Long koadernoaId, LocalDate data);
    List<IkasleEgunOharra> findByIkasleaIdAndKoadernoaIdAndDataBetween(
            Long ikasleaId, Long koadernoaId, LocalDate hasiera, LocalDate amaiera);
    List<IkasleEgunOharra> findByKoadernoaIdAndDataBetween(Long koadernoaId, LocalDate hasiera, LocalDate amaiera);
    List<IkasleEgunOharra> findByKoadernoaIdAndData(Long koadernoaId, LocalDate data);

    @EntityGraph(attributePaths = {"ikaslea", "koadernoa", "koadernoa.moduloa"})
    List<IkasleEgunOharra> findByKoadernoa_Moduloa_Taldea_IdAndKoadernoa_Egutegia_Ikasturtea_IdAndDataBetween(
            Long taldeaId, Long ikasturteaId, LocalDate hasiera, LocalDate amaiera);
}
