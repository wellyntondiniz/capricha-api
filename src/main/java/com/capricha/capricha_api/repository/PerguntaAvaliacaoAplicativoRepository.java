package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.capricha.capricha_api.entidade.PerguntaAvaliacaoAplicativo;

public interface PerguntaAvaliacaoAplicativoRepository
        extends JpaRepository<PerguntaAvaliacaoAplicativo, Integer> {
    List<PerguntaAvaliacaoAplicativo> findByAtivaTrueOrderByOrdemAscIdAsc();
    boolean existsByTexto(String texto);
}
