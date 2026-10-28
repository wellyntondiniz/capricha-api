package com.capricha.capricha_api.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capricha.capricha_api.entidade.RespostaAvaliacaoAplicativo;

public interface RespostaAvaliacaoAplicativoRepository
        extends JpaRepository<RespostaAvaliacaoAplicativo, Long> {
    boolean existsByUsuarioId(Integer usuarioId);
}
