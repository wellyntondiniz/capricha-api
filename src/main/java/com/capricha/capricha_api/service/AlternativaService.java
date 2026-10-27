package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capricha.capricha_api.repository.AlternativaRepository;
import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Pergunta;

@Service
public class AlternativaService {
	
	@Autowired
	private AlternativaRepository alternativaRepository;
	
	public List<Alternativa> getAlternativas() {
		return alternativaRepository.findAll();
	}
	
	public Alternativa getAlternativaById(Integer id) {
		Alternativa alternativa = alternativaRepository.findById(id).get();
		return alternativa;
	}
	
	public Alternativa salvar(Alternativa alternativa) {
		return alternativaRepository.save(alternativa);
	}
	
	public Alternativa updateAlternativa(Integer id, Alternativa dados) {
		Alternativa alternativa = alternativaRepository.findById(id).get();
		alternativa.setTexto(dados.getTexto());
		alternativa.setCorreta(dados.getCorreta());
		return alternativaRepository.save(alternativa);
	}
}
