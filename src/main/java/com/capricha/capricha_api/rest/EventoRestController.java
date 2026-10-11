package com.capricha.capricha_api.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.service.EventoService;
import com.capricha.capricha_api.service.PalestraService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/evento")
@CrossOrigin
public class EventoRestController {

	@Autowired
	EventoService eventoService;

	@Autowired
	PalestraService palestraService;

	@GetMapping
	public List<Evento> getEventos() {
		return eventoService.getEventos();
	}

	@GetMapping("/{id}")
	public Evento getEventoById(@PathVariable Integer id) {
		return eventoService.getEventoById(id);
	}

	@GetMapping("/{id}/palestras")
	public List<Palestra> getPalestrasDoEvento(@PathVariable Integer id) {
		return palestraService.listarPorEvento(id);
	}

	@PostMapping
	public Evento salvar(@Valid @RequestBody Evento evento) {
		return eventoService.cadastrarEvento(evento);
	}

	@PutMapping("/{id}")
	public Evento atualizar(@PathVariable Integer id, @Valid @RequestBody Evento evento) {
		return eventoService.atualizarEvento(id, evento);
	}

	@PutMapping("/{id}/desativar")
	public Evento desativar(@PathVariable Integer id) {
		return eventoService.desativarEvento(id);
	}
}