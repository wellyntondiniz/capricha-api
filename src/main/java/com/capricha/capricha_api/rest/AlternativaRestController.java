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

import com.capricha.capricha_api.service.AlternativaService;

import jakarta.validation.Valid;

import com.capricha.capricha_api.entidade.Alternativa;

@RestController
@RequestMapping(value="/alternativa")
@CrossOrigin
public class AlternativaRestController {
	
	@Autowired
	AlternativaService alternativaService;
	
	@GetMapping
	public List<Alternativa> getAlternativas() {
		return alternativaService.getAlternativas();
	}
	
	@PostMapping
	public Alternativa salvar(@Valid @RequestBody Alternativa alternativa) {
		return alternativaService.salvar(alternativa);
	}
	
	@PutMapping("/{id}")
	public Alternativa updatePergunta(@PathVariable Integer id, @RequestBody Alternativa alternativa) {
		return alternativaService.updateAlternativa(id, alternativa);
	}
}
