package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Participacao;

@Repository
public interface ParticipacaoRepository extends JpaRepository<Participacao, Integer> {

	List<Participacao> findByUsuarioIdAndEventoIdOrderByIdAsc(Integer usuarioId, Integer eventoId);
	List<Participacao> findByUsuarioIdAndCheckedInAtIsNotNull(Integer usuarioId);
	
	public List<Participacao> findByUsuarioAtivoTrueAndEventoAtivoTrue();
	
	public List<Participacao> findByUsuarioIdAndEventoAtivoTrue(Integer usuarioId);

	public List<Participacao> findByEventoIdAndUsuarioAtivoTrue(Integer eventoId);
	
	// Ranking do evento: maior pontuação primeiro; empate resolvido pelo nome.
	public List<Participacao> findByEventoIdAndUsuarioAtivoTrueOrderByScoreDescUsuarioNomeAsc(Integer eventoId);
	
	public Participacao findByUsuarioIdAndEventoId(Integer usuarioId, Integer eventoId);
}
