package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.model.RespostaQuiz;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RespostaQuizRepository extends JpaRepository<RespostaQuiz, Long> {

    boolean existsByUsuarioIdAndQuizId(Integer usuarioId, Long quizId);

    List<RespostaQuiz> findAllByUsuarioId(Integer usuarioId);
}
