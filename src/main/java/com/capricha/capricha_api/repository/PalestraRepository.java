package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.model.Palestra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PalestraRepository extends JpaRepository<Palestra, Long> {

    boolean existsByPalestranteAndDataAndHorario(String palestrante, String data, String horario);

    boolean existsByNomeAndDataAndHorarioAndPalestrante(String nome, String data, String horario, String palestrante);

    // Variações que ignoram a própria palestra (usadas na edição).
    boolean existsByPalestranteAndDataAndHorarioAndIdNot(String palestrante, String data, String horario, Long id);

    boolean existsByNomeAndDataAndHorarioAndPalestranteAndIdNot(String nome, String data, String horario, String palestrante, Long id);

    List<Palestra> findAllByOrderByIdDesc();

    List<Palestra> findByEventoId(Integer eventoId);

    List<Palestra> findByEventoAtivoTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Palestra p where p.id = :id")
    Optional<Palestra> buscarParaCheckIn(@Param("id") Long id);
}