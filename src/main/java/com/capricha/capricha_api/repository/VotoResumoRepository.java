package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.VotoResumo;

@Repository
public interface VotoResumoRepository extends JpaRepository<VotoResumo, Long> {

	List<VotoResumo> findByUsuarioIdAndEventoIdOrderByIdAsc(Integer usuarioId, Integer eventoId);

	long countByUsuarioIdAndEventoId(Integer usuarioId, Integer eventoId);

	boolean existsByUsuarioIdAndResumoId(Integer usuarioId, Integer resumoId);
}