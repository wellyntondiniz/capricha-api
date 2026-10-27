package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Resumo;

@Repository
public interface ResumoRepository extends JpaRepository<Resumo, Integer> {

	List<Resumo> findByEventoIdOrderByTituloAsc(Integer eventoId);

	boolean existsByEventoIdAndTituloIgnoreCase(Integer eventoId, String titulo);

	boolean existsByEventoIdAndTituloIgnoreCaseAndIdNot(Integer eventoId, String titulo, Integer id);
}
