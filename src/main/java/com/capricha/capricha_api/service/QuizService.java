package com.capricha.capricha_api.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Participacao;
import com.capricha.capricha_api.entidade.Pergunta;
import com.capricha.capricha_api.entidade.Quiz;
import com.capricha.capricha_api.entidade.RespostaQuiz;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.PalestraRepository;
import com.capricha.capricha_api.repository.ParticipacaoRepository;
import com.capricha.capricha_api.repository.PresencaPalestraRepository;
import com.capricha.capricha_api.repository.QuizRepository;
import com.capricha.capricha_api.repository.RespostaQuizRepository;
import com.capricha.capricha_api.rest.CheckInException;

/**
 * Quiz das palestras. Regras:
 * <ul>
 * <li>só responde quem fez check-in no evento E na palestra do quiz;</li>
 * <li>cada usuário responde cada quiz uma única vez;</li>
 * <li>o gabarito nunca sai da API e a nota é calculada aqui;</li>
 * <li>os pontos entram no ranking do evento na mesma transação que grava a resposta.</li>
 * </ul>
 */
@Service
public class QuizService {

	public static final int TITULO_MINIMO = 3;
	public static final int TEXTO_MAXIMO = 255;
	public static final int PONTUACAO_MAXIMA = 1000;

	public static final String EVENTO_SEM_CHECK_IN = "EVENT_CHECK_IN_REQUIRED";
	public static final String PALESTRA_SEM_CHECK_IN = "LECTURE_CHECK_IN_REQUIRED";
	public static final String SEM_PERGUNTAS = "QUIZ_EMPTY";
	public static final String PALESTRA_JA_TEM_QUIZ =
			"Esta palestra já tem um quiz cadastrado. Edite o quiz existente para alterar as perguntas.";

	public record AlternativaDto(Integer id, String texto) {}
	public record PerguntaDto(Integer id, String enunciado, List<AlternativaDto> alternativas) {}
	public record PalestraDto(Long id, String nome, Integer eventoId) {}
	public record QuizDto(Long id, String titulo, String descricao, int pontuacao, PalestraDto palestra,
			int totalPerguntas, List<PerguntaDto> perguntas, boolean respondido, boolean disponivel,
			String bloqueio, String mensagemBloqueio) {}
	public record ResultadoDto(Long quizId, int acertos, int totalPerguntas, int pontosObtidos) {}
	public record RespostaPergunta(Integer perguntaId, List<Integer> alternativaIds) {}
	public record NovoQuiz(String titulo, String descricao, Integer pontuacao, Long palestraId) {}
	public record EdicaoQuiz(String titulo, String descricao, Integer pontuacao) {}

	private final QuizRepository quizzes;
	private final RespostaQuizRepository respostas;
	private final PalestraRepository palestras;
	private final EventoRepository eventos;
	private final ParticipacaoRepository participacoes;
	private final PresencaPalestraRepository presencas;
	private final ParticipacaoService participacaoService;

	public QuizService(QuizRepository quizzes, RespostaQuizRepository respostas, PalestraRepository palestras,
			EventoRepository eventos, ParticipacaoRepository participacoes, PresencaPalestraRepository presencas,
			ParticipacaoService participacaoService) {
		this.quizzes = quizzes;
		this.respostas = respostas;
		this.palestras = palestras;
		this.eventos = eventos;
		this.participacoes = participacoes;
		this.presencas = presencas;
		this.participacaoService = participacaoService;
	}

	/**
	 * Lista os quizzes dos eventos ativos. Para cada um informa se o usuário já respondeu e, se não pode responder,
	 * por quê. As perguntas só vão para quem pode responder (ou para o administrador, que as gerencia).
	 */
	@Transactional(readOnly = true)
	public List<QuizDto> listar(Usuario usuario) {
		boolean admin = ehAdmin(usuario);
		Set<Long> respondidos = respostas.findByUsuarioId(usuario.getId()).stream()
				.map(r -> r.getQuiz().getId()).collect(Collectors.toSet());
		Set<Long> palestrasComPresenca = presencas.findByUsuarioId(usuario.getId()).stream()
				.map(p -> p.getPalestra().getId()).collect(Collectors.toSet());
		Set<Integer> eventosComCheckIn = participacoes.findByUsuarioIdAndCheckedInAtIsNotNull(usuario.getId()).stream()
				.map(p -> p.getEvento().getId()).collect(Collectors.toSet());

		return quizzes.findByAtivoTrueOrderByIdAsc().stream()
				.filter(q -> q.getPalestra().getEvento() != null && q.getPalestra().getEvento().getAtivo())
				.map(q -> montar(q, admin, respondidos.contains(q.getId()), eventosComCheckIn, palestrasComPresenca))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ResultadoDto> minhasRespostas(Usuario usuario) {
		return respostas.findByUsuarioId(usuario.getId()).stream()
				.map(r -> new ResultadoDto(r.getQuiz().getId(), r.getAcertos(), r.getTotalPerguntas(), r.getPontosObtidos()))
				.toList();
	}

	@Transactional
	public QuizDto criar(Usuario usuario, NovoQuiz dados) {
		exigirAdmin(usuario, "Somente administradores podem cadastrar quizzes.");
		String titulo = validarTitulo(dados.titulo());
		String descricao = validarDescricao(dados.descricao());
		validarPontuacao(dados.pontuacao());
		if (dados.palestraId() == null) {
			throw erro(HttpStatus.BAD_REQUEST, "Selecione a palestra do quiz.");
		}
		// Bloqueia a linha da palestra: dois cadastros simultâneos para a mesma palestra passam um de cada vez.
		Palestra palestra = palestras.buscarParaCheckIn(dados.palestraId())
				.orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Palestra não encontrada."));
		if (palestra.getEvento() == null || !palestra.getEvento().getAtivo()) {
			throw erro(HttpStatus.CONFLICT, "A palestra não pertence a um evento ativo.");
		}
		if (quizzes.existsByPalestraIdAndAtivoTrue(palestra.getId())) {
			throw erro(HttpStatus.CONFLICT, PALESTRA_JA_TEM_QUIZ);
		}
		if (perguntasValidas(palestra).isEmpty()) {
			throw erro(HttpStatus.BAD_REQUEST,
					"Cadastre ao menos uma pergunta com pelo menos duas alternativas e uma correta nesta palestra antes de criar o quiz.");
		}
		Quiz quiz = quizzes.save(new Quiz(titulo, descricao.isEmpty() ? null : descricao, dados.pontuacao(), palestra));
		return montar(quiz, true, false, Set.of(), Set.of());
	}

	/**
	 * Edita título, descrição e pontuação. As perguntas do quiz são as da palestra e se alteram pelo
	 * cadastro de perguntas; quem já respondeu mantém os pontos que recebeu.
	 */
	@Transactional
	public QuizDto atualizar(Usuario usuario, Long id, EdicaoQuiz dados) {
		exigirAdmin(usuario, "Somente administradores podem editar quizzes.");
		Quiz quiz = quizzes.findById(id).filter(Quiz::isAtivo)
				.orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Quiz não encontrado."));
		String titulo = validarTitulo(dados.titulo());
		String descricao = validarDescricao(dados.descricao());
		validarPontuacao(dados.pontuacao());
		quiz.atualizar(titulo, descricao.isEmpty() ? null : descricao, dados.pontuacao());
		return montar(quizzes.save(quiz), true, false, Set.of(), Set.of());
	}

	/**
	 * Corrige e registra o quiz. Tudo acontece em uma transação: se o ranking não puder ser atualizado,
	 * a resposta também não é gravada (e o usuário pode tentar de novo).
	 */
	@Transactional
	public ResultadoDto responder(Long quizId, Usuario usuario, List<RespostaPergunta> envio) {
		Quiz quiz = quizzes.findById(quizId).filter(Quiz::isAtivo)
				.orElseThrow(() -> new CheckInException(404, "QUIZ_NOT_FOUND", "Quiz não encontrado."));
		Palestra palestra = quiz.getPalestra();
		if (palestra.getEvento() == null) {
			throw new CheckInException(409, "EVENT_NOT_ACTIVE", "Este quiz não pertence a um evento ativo.");
		}
		// Bloqueia a linha do evento: respostas simultâneas do mesmo evento passam uma de cada vez.
		Evento evento = eventos.buscarParaCheckIn(palestra.getEvento().getId())
				.orElseThrow(() -> new CheckInException(404, "EVENT_NOT_FOUND", "Evento não encontrado."));
		if (!evento.getAtivo()) {
			throw new CheckInException(409, "EVENT_NOT_ACTIVE", "Este evento está desativado e não aceita respostas.");
		}
		if (!fezCheckInNoEvento(usuario, evento.getId())) {
			throw new CheckInException(403, EVENTO_SEM_CHECK_IN, mensagemEvento());
		}
		if (presencas.findByUsuarioIdAndPalestraId(usuario.getId(), palestra.getId()).isEmpty()) {
			throw new CheckInException(403, PALESTRA_SEM_CHECK_IN, mensagemPalestra(palestra));
		}
		if (respostas.existsByUsuarioIdAndQuizId(usuario.getId(), quiz.getId())) {
			throw jaRespondido();
		}
		List<Pergunta> perguntas = perguntasValidas(palestra);
		if (perguntas.isEmpty()) {
			throw new CheckInException(409, SEM_PERGUNTAS, "Este quiz ainda não tem perguntas disponíveis.");
		}

		Map<Integer, Set<Integer>> escolhas = lerEscolhas(envio, perguntas);
		int acertos = 0;
		for (Pergunta pergunta : perguntas) {
			Set<Integer> corretas = pergunta.getAlternativas().stream().filter(Alternativa::getCorreta)
					.map(Alternativa::getId).collect(Collectors.toSet());
			if (corretas.equals(escolhas.get(pergunta.getId()))) {
				acertos++;
			}
		}
		int total = perguntas.size();
		int pontos = quiz.getPontuacao() * acertos / total;

		try {
			respostas.saveAndFlush(new RespostaQuiz(usuario, quiz, acertos, total, pontos, Instant.now()));
		} catch (DataIntegrityViolationException e) {
			throw jaRespondido();
		}
		if (pontos > 0) {
			participacaoService.atualizarScore(usuario.getId(), evento.getId(), pontos);
		}
		return new ResultadoDto(quiz.getId(), acertos, total, pontos);
	}

	/** Cada pergunta do quiz precisa de resposta, com alternativas que realmente pertençam a ela. */
	private Map<Integer, Set<Integer>> lerEscolhas(List<RespostaPergunta> envio, List<Pergunta> perguntas) {
		if (envio == null || envio.isEmpty()) {
			throw erro(HttpStatus.BAD_REQUEST, "Responda todas as perguntas antes de enviar.");
		}
		Map<Integer, Pergunta> porId = new HashMap<>();
		perguntas.forEach(p -> porId.put(p.getId(), p));
		Map<Integer, Set<Integer>> escolhas = new HashMap<>();
		for (RespostaPergunta resposta : envio) {
			Pergunta pergunta = resposta == null || resposta.perguntaId() == null ? null : porId.get(resposta.perguntaId());
			if (pergunta == null) {
				throw erro(HttpStatus.BAD_REQUEST, "Há uma pergunta que não pertence a este quiz.");
			}
			if (escolhas.containsKey(pergunta.getId())) {
				throw erro(HttpStatus.BAD_REQUEST, "A mesma pergunta foi respondida mais de uma vez.");
			}
			Set<Integer> permitidas = pergunta.getAlternativas().stream().map(Alternativa::getId).collect(Collectors.toSet());
			Set<Integer> marcadas = new HashSet<>(resposta.alternativaIds() == null ? List.of() : resposta.alternativaIds());
			if (marcadas.isEmpty()) {
				throw erro(HttpStatus.BAD_REQUEST, "Responda todas as perguntas antes de enviar.");
			}
			if (!permitidas.containsAll(marcadas)) {
				throw erro(HttpStatus.BAD_REQUEST, "Há uma alternativa que não pertence à pergunta.");
			}
			escolhas.put(pergunta.getId(), marcadas);
		}
		if (escolhas.size() != perguntas.size()) {
			throw erro(HttpStatus.BAD_REQUEST, "Responda todas as perguntas antes de enviar.");
		}
		return escolhas;
	}

	private QuizDto montar(Quiz quiz, boolean admin, boolean respondido, Set<Integer> eventosComCheckIn,
			Set<Long> palestrasComPresenca) {
		Palestra palestra = quiz.getPalestra();
		List<Pergunta> perguntas = perguntasValidas(palestra);
		String bloqueio = null;
		String mensagem = null;
		if (!respondido && !admin) {
			if (palestra.getEvento() == null || !eventosComCheckIn.contains(palestra.getEvento().getId())) {
				bloqueio = EVENTO_SEM_CHECK_IN;
				mensagem = mensagemEvento();
			} else if (!palestrasComPresenca.contains(palestra.getId())) {
				bloqueio = PALESTRA_SEM_CHECK_IN;
				mensagem = mensagemPalestra(palestra);
			} else if (perguntas.isEmpty()) {
				bloqueio = SEM_PERGUNTAS;
				mensagem = "Este quiz ainda não tem perguntas disponíveis.";
			}
		}
		boolean disponivel = !respondido && bloqueio == null && !perguntas.isEmpty();
		List<PerguntaDto> visiveis = disponivel || admin
				? perguntas.stream().map(p -> new PerguntaDto(p.getId(), p.getEnunciado(),
						p.getAlternativas().stream().map(a -> new AlternativaDto(a.getId(), a.getTexto())).toList())).toList()
				: List.of();
		Integer eventoId = palestra.getEvento() == null ? null : palestra.getEvento().getId();
		return new QuizDto(quiz.getId(), quiz.getTitulo(), quiz.getDescricao(), quiz.getPontuacao(),
				new PalestraDto(palestra.getId(), palestra.getNome(), eventoId), perguntas.size(), visiveis,
				respondido, disponivel, bloqueio, mensagem);
	}

	/** Só entram no quiz perguntas ativas, com pelo menos duas alternativas e ao menos uma correta. */
	private List<Pergunta> perguntasValidas(Palestra palestra) {
		return palestra.getPerguntas().stream()
				.filter(Pergunta::getAtivo)
				.filter(p -> p.getAlternativas() != null && p.getAlternativas().size() >= 2
						&& p.getAlternativas().stream().anyMatch(Alternativa::getCorreta))
				.toList();
	}

	private boolean fezCheckInNoEvento(Usuario usuario, Integer eventoId) {
		return participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), eventoId).stream()
				.map(Participacao::getCheckedInAt).anyMatch(java.util.Objects::nonNull);
	}

	private void exigirAdmin(Usuario usuario, String mensagem) {
		if (!ehAdmin(usuario)) {
			throw new CheckInException(403, "FORBIDDEN", mensagem);
		}
	}

	private String validarTitulo(String bruto) {
		String titulo = bruto == null ? "" : bruto.trim();
		if (titulo.isEmpty()) {
			throw erro(HttpStatus.BAD_REQUEST, "É necessário informar o título do quiz.");
		}
		if (titulo.length() < TITULO_MINIMO || titulo.length() > TEXTO_MAXIMO) {
			throw erro(HttpStatus.BAD_REQUEST, "O título deve ter entre " + TITULO_MINIMO + " e " + TEXTO_MAXIMO + " caracteres.");
		}
		return titulo;
	}

	private String validarDescricao(String bruta) {
		String descricao = bruta == null ? "" : bruta.trim();
		if (descricao.length() > TEXTO_MAXIMO) {
			throw erro(HttpStatus.BAD_REQUEST, "A descrição deve ter no máximo " + TEXTO_MAXIMO + " caracteres.");
		}
		return descricao;
	}

	private void validarPontuacao(Integer pontuacao) {
		if (pontuacao == null || pontuacao < 1 || pontuacao > PONTUACAO_MAXIMA) {
			throw erro(HttpStatus.BAD_REQUEST, "A pontuação deve ser um inteiro entre 1 e " + PONTUACAO_MAXIMA + ".");
		}
	}

	private boolean ehAdmin(Usuario usuario) {
		return "ADMIN".equalsIgnoreCase(usuario.getPerfil()) || "ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil());
	}

	private String mensagemEvento() {
		return "Faça o check-in no evento para responder este quiz.";
	}

	private String mensagemPalestra(Palestra palestra) {
		return "Faça o check-in na palestra \"" + palestra.getNome() + "\" para responder este quiz.";
	}

	private CheckInException jaRespondido() {
		return new CheckInException(409, "ALREADY_ANSWERED", "Você já respondeu este quiz.");
	}

	private ResponseStatusException erro(HttpStatus status, String mensagem) {
		return new ResponseStatusException(status, mensagem);
	}
}