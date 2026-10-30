package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.model.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    boolean existsByPalestraIdAndTituloIgnoreCase(Long palestraId, String titulo);
}
