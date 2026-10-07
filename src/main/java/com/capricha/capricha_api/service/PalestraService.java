package com.capricha.capricha_api.service;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.PalestraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PalestraService {

    private static final ZoneId FUSO = ZoneId.of("America/Cuiaba");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final PalestraRepository palestraRepository;
    private final EventoRepository eventoRepository;

    public PalestraService(PalestraRepository palestraRepository, EventoRepository eventoRepository) {
        this.palestraRepository = palestraRepository;
        this.eventoRepository = eventoRepository;
    }

    public Palestra salvar(Palestra palestra) {
        if (vazio(palestra.getNome()) || vazio(palestra.getPalestrante())
                || vazio(palestra.getData()) || vazio(palestra.getHorario())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nome, palestrante, data e horário são obrigatórios!");
        }

        if (palestra.getEvento() == null || palestra.getEvento().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É necessário informar o evento!");
        }

        Evento evento = eventoRepository.findById(palestra.getEvento().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado!"));
        palestra.setEvento(evento);

        validarNaoPassou(palestra);

        if (palestraRepository.existsByPalestranteIgnoreCaseAndDataAndHorario(
                palestra.getPalestrante().trim(), palestra.getData().trim(), palestra.getHorario().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este palestrante já tem uma palestra cadastrada nesta data e horário.");
        }

        return palestraRepository.save(palestra);
    }

    public List<Palestra> listar() {
        return palestraRepository.findAll();
    }

    public List<Palestra> listarPorEvento(Integer eventoId) {
        return palestraRepository.findByEventoId(eventoId);
    }

    private void validarNaoPassou(Palestra palestra) {
        try {
            LocalDateTime dataHora = LocalDateTime.parse(
                    palestra.getData().trim() + " " + palestra.getHorario().trim(), FORMATO);
            LocalDateTime agora = LocalDateTime.now(FUSO).truncatedTo(ChronoUnit.MINUTES);

            if (dataHora.isBefore(agora)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A palestra não pode ser cadastrada em uma data e horário que já passaram.");
            }
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data ou horário inválido. Use DD/MM/AAAA e HH:MM.");
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}