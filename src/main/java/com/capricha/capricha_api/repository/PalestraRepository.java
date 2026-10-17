package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.model.Palestra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PalestraRepository extends JpaRepository<Palestra, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Palestra p where p.id = :id")
    java.util.Optional<Palestra> buscarParaCheckIn(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "evento")
    List<Palestra> findByEventoAtivoTrue();

    List<Palestra> findByEventoId(Integer eventoId);

    boolean existsByPalestranteIgnoreCaseAndDataAndHorario(String palestrante, String data, String horario);
}
