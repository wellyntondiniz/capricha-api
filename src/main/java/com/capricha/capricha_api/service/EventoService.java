package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.repository.EventoRepository;

@Service
public class EventoService {

	@Autowired
	private EventoRepository eventoRepository;

	public List<Evento> getEventos() {
		return eventoRepository.findAllByAtivo(true);
	}

	public Evento getEventoById(Integer id) {
		return eventoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(
						HttpStatus.NOT_FOUND,
						"Evento não encontrado!"
				));
	}

	public Evento cadastrarEvento(Evento evento) {
		validar(evento);
		evento.setAtivo(true);
		return salvar(evento);
	}

	public Evento atualizarEvento(Integer id, Evento dados) {
		validar(dados);
		Evento evento = getEventoById(id);
		evento.setNome(dados.getNome());
		evento.setDescricao(dados.getDescricao());
		evento.setDataInicio(dados.getDataInicio());
		evento.setDataTermino(dados.getDataTermino());
		return salvar(evento);
	}

	public Evento desativarEvento(Integer id) {
		Evento evento = getEventoById(id);
		evento.setAtivo(false);
		return salvar(evento);
	}

	public Evento salvar(Evento evento) {
		return eventoRepository.save(evento);
	}

	private void validar(Evento evento) {
		if (evento.getDataInicio() != null && evento.getDataTermino() != null) {
			if (evento.getDataTermino().isBefore(evento.getDataInicio())) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Data de término não pode ser anterior à data de início!"
				);
			}
		}

		if (evento.getDataTermino() != null && evento.getDataInicio() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Data de término exige data de início!"
			);
		}
	}
}
