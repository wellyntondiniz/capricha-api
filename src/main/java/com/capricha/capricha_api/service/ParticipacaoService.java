package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.repository.ParticipacaoRepository;
import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Participacao;
import com.capricha.capricha_api.entidade.Usuario;

@Service
public class ParticipacaoService {
	
	@Autowired
	private ParticipacaoRepository participacaoRepository;

	@Autowired
	private com.capricha.capricha_api.repository.EventoRepository eventoRepository;
	
	public List<Participacao> getParticipacoes() {
		return participacaoRepository.findByUsuarioAtivoTrueAndEventoAtivoTrue();
	}
	
	public List<Participacao> getParticipacoesByUsuario(Integer usuarioId) {
		return participacaoRepository.findByUsuarioIdAndEventoAtivoTrue(usuarioId);
	}
	
	public List<Participacao> getParticipacoesByEvento(Integer eventoId) {
		return participacaoRepository.findByEventoIdAndUsuarioAtivoTrueOrderByScoreDescUsuarioNomeAsc(eventoId);
	}
	
	public Participacao getParticipacaoById(Integer id) {
		Participacao participacao = participacaoRepository.findById(id).get();
		return participacao;
	}
	
	@org.springframework.transaction.annotation.Transactional
	public Participacao inscricao(Participacao participacao) {
		if (participacao.getEvento() == null || participacao.getEvento().getId() == null
				|| participacao.getUsuario() == null || participacao.getUsuario().getId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe usuário e evento.");
		}
		Evento evento = eventoRepository.buscarParaCheckIn(participacao.getEvento().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
		var existentes = participacaoRepository.findByUsuarioIdAndEventoIdOrderByIdAsc(
				participacao.getUsuario().getId(), evento.getId());
		// Uma inscrição não pode sobrescrever presença/pontos nem criar outra participação do mesmo par.
		if (!existentes.isEmpty()) return existentes.get(0);
		participacao.setId(null);
		participacao.setEvento(evento);
		participacao.setCheckedInAt(null);
		participacao.setScore(0);
		return participacaoRepository.save(participacao);
	}
	
	@org.springframework.transaction.annotation.Transactional
	public Participacao atualizarScore(Integer usuarioId, Integer eventoId, Integer score) {
		// Evita salvar uma cópia anterior ao check-in e apagar sua data de presença.
		eventoRepository.buscarParaCheckIn(eventoId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado."));
		Participacao participacao = participacaoRepository.findByUsuarioIdAndEventoId(usuarioId, eventoId);
		if (participacao == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada.");
		}
		
		score = participacao.getScore() + score;
		
		participacao.setScore(score);
		return participacaoRepository.save(participacao);
	}
}
