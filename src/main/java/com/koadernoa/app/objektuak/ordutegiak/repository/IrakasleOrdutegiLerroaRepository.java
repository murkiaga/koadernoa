package com.koadernoa.app.objektuak.ordutegiak.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.koadernoa.app.objektuak.ordutegiak.entitateak.IrakasleOrdutegiLerroa;

public interface IrakasleOrdutegiLerroaRepository extends JpaRepository<IrakasleOrdutegiLerroa, Long> {
    @EntityGraph(attributePaths = {"irakasleOrdutegia", "irakasleOrdutegia.irakaslea"})
    Optional<IrakasleOrdutegiLerroa> findWithIrakasleOrdutegiaById(Long id);
}
