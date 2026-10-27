package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Evento;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Integer> {

	@org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@org.springframework.data.jpa.repository.Query("select e from Evento e where e.id = :id")
	java.util.Optional<Evento> buscarParaCheckIn(@org.springframework.data.repository.query.Param("id") Integer id);

	public List<Evento> findAllByAtivo(Boolean ativo);

}
