package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Evento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Integer> {

	public List<Evento> findAllByAtivo(Boolean ativo);

}
