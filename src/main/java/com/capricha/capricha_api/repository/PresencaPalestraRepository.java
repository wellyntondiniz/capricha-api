package com.capricha.capricha_api.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import com.capricha.capricha_api.entidade.PresencaPalestra;

public interface PresencaPalestraRepository extends JpaRepository<PresencaPalestra, Long> {
    Optional<PresencaPalestra> findByUsuarioIdAndPalestraId(Integer usuarioId, Long palestraId);

    @EntityGraph(attributePaths = "palestra")
    List<PresencaPalestra> findByUsuarioId(Integer usuarioId);

    boolean existsByPalestraId(Long palestraId);
}