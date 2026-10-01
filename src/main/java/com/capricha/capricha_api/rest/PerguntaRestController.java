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

import com.capricha.capricha_api.service.PerguntaService;

import jakarta.validation.Valid;

import com.capricha.capricha_api.entidade.Pergunta;

@RestController
@RequestMapping(value="/pergunta")
@CrossOrigin
public class PerguntaRestController {
	
	@Autowired
	PerguntaService perguntaService;
	
	@GetMapping
	public List<Pergunta> getPergunta() {
		return perguntaService.getPerguntas();
	}
	
	@PostMapping
	public Pergunta salvar(@Valid @RequestBody Pergunta pergunta) {
		return perguntaService.salvar(pergunta);
	}
	
	@PutMapping("/{id}")
	public Pergunta updatePergunta(@PathVariable Integer id, @RequestBody Pergunta pergunta) {
		return perguntaService.updatePergunta(id, pergunta);
	}
}
