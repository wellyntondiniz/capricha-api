package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capricha.capricha_api.entidade.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

	List<Quiz> findByAtivoTrueOrderByIdAsc();

	/** Cada palestra tem no máximo um quiz ativo. */
	boolean existsByPalestraIdAndAtivoTrue(Long palestraId);
}