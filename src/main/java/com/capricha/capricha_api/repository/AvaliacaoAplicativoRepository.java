package com.capricha.capricha_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capricha.capricha_api.entidade.AvaliacaoAplicativo;

public interface AvaliacaoAplicativoRepository extends JpaRepository<AvaliacaoAplicativo, Long> {
    boolean existsByUsuarioId(Integer usuarioId);
}
