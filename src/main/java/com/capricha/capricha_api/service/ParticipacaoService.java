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
	
	public List<Participacao> getParticipacoes() {
		return participacaoRepository.findByUsuarioAtivoTrueAndEventoAtivoTrue();
	}
	
	public List<Participacao> getParticipacoesByUsuario(Integer usuarioId) {
		return participacaoRepository.findByUsuarioIdAndEventoAtivoTrue(usuarioId);
	}
	
	public List<Participacao> getParticipacoesByEvento(Integer eventoId) {
		return participacaoRepository.findByEventoIdAndUsuarioAtivoTrue(eventoId);
	}
	
	public Participacao getParticipacaoById(Integer id) {
		Participacao participacao = participacaoRepository.findById(id).get();
		return participacao;
	}
	
	public Participacao inscricao(Participacao participacao) {
		participacao.setScore(0);
		return participacaoRepository.save(participacao);
	}
	
	public Participacao atualizarScore(Integer usuarioId, Integer eventoId, Integer score) {
		Participacao participacao = participacaoRepository.findByUsuarioIdAndEventoId(usuarioId, eventoId);
		
		score = participacao.getScore() + score;
		
		participacao.setScore(score);
		return participacaoRepository.save(participacao);
	}
}