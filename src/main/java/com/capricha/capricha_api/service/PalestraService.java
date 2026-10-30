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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PalestraService {

    // Tamanho padrão das colunas de texto (VARCHAR 255)
    private static final int TAMANHO_MAXIMO_TEXTO = 255;

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
        validarPergunta(pergunta, palestra);

        pergunta.setEnunciado(pergunta.getEnunciado().trim());
        for (Alternativa alternativa : pergunta.getAlternativas()) {
            alternativa.setTexto(alternativa.getTexto().trim());
        }
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

    private void validarPergunta(Pergunta pergunta, Palestra palestra) {
        if (pergunta == null || vazio(pergunta.getEnunciado())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Digite o enunciado da pergunta.");
        }
        if (pergunta.getEnunciado().trim().length() > TAMANHO_MAXIMO_TEXTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O enunciado pode ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }
        String enunciado = pergunta.getEnunciado().trim();
        boolean enunciadoRepetido = palestra.getPerguntas().stream()
                .anyMatch(existente -> existente.getEnunciado() != null
                        && existente.getEnunciado().trim().equalsIgnoreCase(enunciado));
        if (enunciadoRepetido) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta palestra já tem uma pergunta com este enunciado.");
        }

        List<Alternativa> alternativas = pergunta.getAlternativas();
        if (alternativas == null || alternativas.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe pelo menos duas alternativas.");
        }
        Set<String> textos = new HashSet<>();
        for (Alternativa alternativa : alternativas) {
            if (alternativa == null || vazio(alternativa.getTexto())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Preencha o texto de todas as alternativas.");
            }
            if (alternativa.getTexto().trim().length() > TAMANHO_MAXIMO_TEXTO) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada alternativa pode ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
            }
            if (!textos.add(alternativa.getTexto().trim().toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "As alternativas não podem se repetir.");
            }
        }
        long corretas = alternativas.stream().filter(Alternativa::getCorreta).count();
        if (corretas == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Marque pelo menos uma alternativa correta.");
        }
        if (corretas == alternativas.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pelo menos uma alternativa precisa ser incorreta.");
        }
    }

    private boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}
