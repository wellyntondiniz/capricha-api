package com.capricha.capricha_api.service;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Pergunta;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.PalestraRepository;
import com.capricha.capricha_api.repository.PresencaPalestraRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

@Service
public class PalestraService {

    private static final ZoneId FUSO = ZoneId.of("America/Cuiaba");
    // STRICT: rejeita datas inexistentes (ex.: 31/02/2026) em vez de "arredondar".
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    // Id usado no cadastro: nenhuma palestra tem id negativo, então nada é ignorado na checagem de conflito.
    private static final Long SEM_ID = -1L;

    private final PalestraRepository palestraRepository;
    private final EventoRepository eventoRepository;
    private final PresencaPalestraRepository presencaRepository;
    private final ArmazenamentoArquivoService armazenamento;

    public PalestraService(
            PalestraRepository palestraRepository,
            EventoRepository eventoRepository,
            PresencaPalestraRepository presencaRepository,
            ArmazenamentoArquivoService armazenamento
    ) {
        this.palestraRepository = palestraRepository;
        this.eventoRepository = eventoRepository;
        this.presencaRepository = presencaRepository;
        this.armazenamento = armazenamento;
    }

    @Transactional
    public Palestra salvar(Palestra palestra) {
        palestra.setId(null);
        normalizar(palestra);
        validarObrigatorios(palestra);

        Evento evento = buscarEventoAtivo(palestra.getEvento());
        palestra.setEvento(evento);

        validarAgenda(palestra, evento, true, true);
        validarConflitos(palestra, SEM_ID);

        return palestraRepository.save(palestra);
    }

    /**
     * Atualiza uma palestra. "Já passou" e o período do evento só são revalidados quando
     * o início/término/evento mudaram, para permitir corrigir uma palestra que já aconteceu.
     *
     * @param novaImagem    nome do arquivo já salvo para a nova imagem (ou null para manter a atual)
     * @param removerImagem apaga a imagem atual quando nenhuma nova foi enviada
     */
    @Transactional
    public Palestra atualizar(Long id, Palestra dados, String novaImagem, boolean removerImagem) {
        Palestra atual = buscarPorId(id);
        normalizar(dados);
        validarObrigatorios(dados);

        Integer eventoIdNovo = dados.getEvento() == null ? null : dados.getEvento().getId();
        boolean eventoMudou = atual.getEvento() == null
                || !Objects.equals(atual.getEvento().getId(), eventoIdNovo);
        Evento evento = eventoMudou ? buscarEventoAtivo(dados.getEvento()) : atual.getEvento();

        boolean inicioMudou = !Objects.equals(limpar(atual.getData()), dados.getData())
                || !Objects.equals(limpar(atual.getHorario()), dados.getHorario());
        boolean agendaMudou = inicioMudou
                || eventoMudou
                || !Objects.equals(atual.getDataTermino(), dados.getDataTermino());

        validarAgenda(dados, evento, inicioMudou, agendaMudou);
        validarConflitos(dados, id);

        atual.setNome(dados.getNome());
        atual.setDescricao(dados.getDescricao());
        atual.setPalestrante(dados.getPalestrante());
        atual.setData(dados.getData());
        atual.setHorario(dados.getHorario());
        atual.setDataTermino(dados.getDataTermino());
        atual.setEvento(evento);

        String imagemAntiga = atual.getImagem();
        if (novaImagem != null) {
            atual.setImagem(novaImagem);
        } else if (removerImagem) {
            atual.setImagem(null);
        }

        Palestra salva = palestraRepository.saveAndFlush(atual);

        if (!Objects.equals(imagemAntiga, salva.getImagem())) {
            armazenamento.remover(imagemAntiga);
        }
        return salva;
    }

    @Transactional
    public void excluir(Long id) {
        Palestra palestra = buscarPorId(id);

        if (presencaRepository.existsByPalestraId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir: já existem check-ins registrados nesta palestra.");
        }

        String imagem = palestra.getImagem();
        palestraRepository.delete(palestra); // as perguntas (e alternativas) saem em cascata
        palestraRepository.flush();
        armazenamento.remover(imagem);
    }

    public List<Palestra> listar() {
        return palestraRepository.findAllByOrderByIdDesc();
    }

    public Palestra buscar(Long id) {
        return buscarPorId(id);
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

    private Evento buscarEventoAtivo(Evento referencia) {
        if (referencia == null || referencia.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione o evento da palestra.");
        }
        Evento evento = eventoRepository.findById(referencia.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Evento não encontrado."));
        if (!evento.getAtivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O evento selecionado está desativado.");
        }
        return evento;
    }

    private void normalizar(Palestra palestra) {
        palestra.setNome(limpar(palestra.getNome()));
        palestra.setDescricao(limpar(palestra.getDescricao()));
        palestra.setPalestrante(limpar(palestra.getPalestrante()));
        palestra.setData(limpar(palestra.getData()));
        palestra.setHorario(limpar(palestra.getHorario()));
    }

    private void validarObrigatorios(Palestra palestra) {
        if (vazio(palestra.getNome())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da palestra é obrigatório.");
        }
        if (vazio(palestra.getPalestrante())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O palestrante é obrigatório.");
        }
        if (vazio(palestra.getData())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data é obrigatória.");
        }
        if (vazio(palestra.getHorario())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O horário é obrigatório.");
        }
    }

    private LocalDateTime lerInicio(Palestra palestra) {
        try {
            return LocalDateTime.parse(palestra.getData() + " " + palestra.getHorario(), FORMATO);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Data ou horário inválido. Use DD/MM/AAAA e HH:MM.");
        }
    }

    private void validarAgenda(Palestra palestra, Evento evento, boolean validarPassado, boolean validarPeriodo) {
        LocalDateTime inicio = lerInicio(palestra);
        LocalDateTime fim = palestra.getDataTermino();

        if (fim != null && !fim.isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O término deve ser posterior ao início da palestra.");
        }

        if (validarPassado) {
            LocalDateTime agora = LocalDateTime.now(FUSO).truncatedTo(ChronoUnit.MINUTES);
            if (inicio.isBefore(agora)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A palestra não pode ser cadastrada em uma data e horário que já passaram.");
            }
        }

        if (validarPeriodo) {
            if (evento.getDataInicio() != null && inicio.isBefore(evento.getDataInicio())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A palestra não pode ocorrer antes do início do evento.");
            }
            if (evento.getDataTermino() != null && inicio.isAfter(evento.getDataTermino())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "A palestra não pode ocorrer depois do término do evento.");
            }
            if (fim != null && evento.getDataTermino() != null && fim.isAfter(evento.getDataTermino())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O término da palestra não pode ultrapassar o término do evento.");
            }
        }
    }

    private void validarConflitos(Palestra palestra, Long idIgnorado) {
        boolean duplicataExata = palestraRepository.existsByNomeAndDataAndHorarioAndPalestranteAndIdNot(
                palestra.getNome(), palestra.getData(), palestra.getHorario(), palestra.getPalestrante(), idIgnorado);

        if (duplicataExata) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta palestra já está cadastrada.");
        }

        boolean conflitoAgenda = palestraRepository.existsByPalestranteAndDataAndHorarioAndIdNot(
                palestra.getPalestrante(), palestra.getData(), palestra.getHorario(), idIgnorado);

        if (conflitoAgenda) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este palestrante já tem uma palestra cadastrada nesta data e horário.");
        }
    }

    private static String limpar(String valor) {
        return valor == null ? "" : valor.trim();
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.trim().isEmpty();
    }
}