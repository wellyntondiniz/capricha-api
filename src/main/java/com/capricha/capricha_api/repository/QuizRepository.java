package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capricha.capricha_api.entidade.Quiz;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

	List<Quiz> findByAtivoTrueOrderByIdAsc();

	boolean existsByPalestraIdAndTituloIgnoreCaseAndAtivoTrue(Long palestraId, String titulo);
}
