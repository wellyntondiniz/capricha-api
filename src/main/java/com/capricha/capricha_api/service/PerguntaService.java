package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capricha.capricha_api.repository.PerguntaRepository;
import com.capricha.capricha_api.entidade.Pergunta;

@Service
public class PerguntaService {
	
	@Autowired
	private PerguntaRepository perguntaRepository;
	
	public List<Pergunta> getPerguntas() {
		return perguntaRepository.findAll();
	}
	
	public Pergunta getPerguntaById(Integer id) {
		Pergunta pergunta= perguntaRepository.findById(id).get();
		return pergunta;
	}
	
	public Pergunta salvar(Pergunta pergunta) {
		return perguntaRepository.save(pergunta);
	}
	
	public Pergunta updatePergunta(Integer id, Pergunta dados) {
		Pergunta pergunta = perguntaRepository.findById(id).get();
		pergunta.setEnunciado(dados.getEnunciado());
		return perguntaRepository.save(pergunta);
	}
}
