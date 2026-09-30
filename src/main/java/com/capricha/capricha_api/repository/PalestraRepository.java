package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.model.Palestra;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PalestraRepository extends JpaRepository<Palestra, Long> {

    boolean existsByPalestranteAndDataAndHorario(String palestrante, String data, String horario);

    boolean existsByNomeAndDataAndHorarioAndPalestrante(String nome, String data, String horario, String palestrante);
}