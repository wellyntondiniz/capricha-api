package com.capricha.capricha_api.rest;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.capricha.capricha_api.service.ParticipacaoService;

import jakarta.validation.Valid;

import com.capricha.capricha_api.entidade.Participacao;

@RestController
@RequestMapping(value="/participacao")
@CrossOrigin
public class ParticipacaoRestController {
	
	@Autowired
	ParticipacaoService participacaoService;
	
	@GetMapping
	public List<Participacao> getParticipacoes() {
		return participacaoService.getParticipacoes();
	}
	
	@GetMapping("/{usuarioId}/byUsuario")
	public List<Participacao> getParticipacoesByUsuario(@PathVariable Integer usuarioId) {
		return participacaoService.getParticipacoesByUsuario(usuarioId);
	}
	
	@GetMapping("/{eventoId}/byUsuario")
	public List<Participacao> getParticipacoesByEvento(@PathVariable Integer eventoId) {
		return participacaoService.getParticipacoesByEvento(eventoId);
	}
	
	@GetMapping("/{id}")
	public Participacao getParticipacoesById(@PathVariable Integer id) {
		return participacaoService.getParticipacaoById(id);
	}
	
	@PostMapping
	public Participacao inscricao(@Valid @RequestBody Participacao participacao) {
		return participacaoService.inscricao(participacao);
	}
	
	@PutMapping("/usuario/{usuarioId}/evento/{eventoId}/score/{score}")
	public Participacao atualizarScore(@PathVariable Integer usuarioId, @PathVariable Integer eventoId, @PathVariable Integer score) {
		return participacaoService.atualizarScore(usuarioId, eventoId, score);
	}
}
