package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capricha.capricha_api.entidade.RespostaQuiz;

public interface RespostaQuizRepository extends JpaRepository<RespostaQuiz, Long> {

	boolean existsByUsuarioIdAndQuizId(Integer usuarioId, Long quizId);

	List<RespostaQuiz> findByUsuarioId(Integer usuarioId);
}
