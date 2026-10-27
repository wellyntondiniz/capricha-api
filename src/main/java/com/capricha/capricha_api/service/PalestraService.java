package com.capricha.capricha_api.service;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Pergunta;
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
        boolean duplicataExata = palestraRepository.existsByNomeAndDataAndHorarioAndPalestrante(
            palestra.getNome(),
            palestra.getData(),
            palestra.getHorario(),
            palestra.getPalestrante()
        );

        if (duplicataExata) {
            throw new IllegalStateException("Esta palestra já está cadastrada.");
        }

        boolean conflitoAgenda = palestraRepository.existsByPalestranteAndDataAndHorario(
            palestra.getPalestrante(),
            palestra.getData(),
            palestra.getHorario()
        );

        if (conflitoAgenda) {
            throw new IllegalStateException(
                "Este palestrante já tem uma palestra cadastrada nesta data e horário."
            );
        }

        return palestraRepository.save(palestra);
    }

    public List<Palestra> listar() {
        return palestraRepository.findAll();
    }

    public Pergunta adicionarPergunta(Long id, Pergunta pergunta) {
        Palestra palestra = buscarPorId(id);

        palestra.adicionarPergunta(pergunta);

        for (Alternativa alternativa : pergunta.getAlternativas()) {
            alternativa.setPergunta(pergunta);
        }

        palestra = palestraRepository.save(palestra);

        return palestra.getPerguntas().get(palestra.getPerguntas().size() - 1);
    }

    public List<Pergunta> listarPerguntas(Long id) {
        return buscarPorId(id).getPerguntas();
    }

    public List<Palestra> listarPorEvento(Integer eventoId) {
        return palestraRepository.findByEventoId(eventoId);
    }

    private Palestra buscarPorId(Long id) {
        return palestraRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Palestra não encontrada!"));
    }

    private void validarNaoPassou(Palestra palestra) {
        try {
            LocalDateTime dataHora = LocalDateTime.parse(
                    palestra.getData().trim() + " " + palestra.getHorario().trim(), FORMATO);
            if (palestra.getDataTermino() != null && !palestra.getDataTermino().isAfter(dataHora)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O término deve ser posterior ao início da palestra.");
            }
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
