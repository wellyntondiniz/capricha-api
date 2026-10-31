package com.capricha.capricha_api.service;

import com.capricha.capricha_api.dto.PontuacaoRankingRequest;
import com.capricha.capricha_api.dto.QuizRequest;
import com.capricha.capricha_api.dto.QuizResponse;
import com.capricha.capricha_api.dto.RespostaQuizRequest;
import com.capricha.capricha_api.dto.ResultadoQuizResponse;
import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Pergunta;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.model.Quiz;
import com.capricha.capricha_api.model.RespostaQuiz;
import com.capricha.capricha_api.repository.PalestraRepository;
import com.capricha.capricha_api.repository.QuizRepository;
import com.capricha.capricha_api.repository.RespostaQuizRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizService {

    // Tamanho padrão das colunas de texto (VARCHAR 255)
    private static final int TAMANHO_MAXIMO_TEXTO = 255;
    private static final int PONTUACAO_MAXIMA = 1000;

    private final QuizRepository quizRepository;
    private final RespostaQuizRepository respostaQuizRepository;
    private final PalestraRepository palestraRepository;
    private final RankingService rankingService;

    public QuizService(QuizRepository quizRepository, RespostaQuizRepository respostaQuizRepository,
                       PalestraRepository palestraRepository, RankingService rankingService) {
        this.quizRepository = quizRepository;
        this.respostaQuizRepository = respostaQuizRepository;
        this.palestraRepository = palestraRepository;
        this.rankingService = rankingService;
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> listar() {
        return quizRepository.findAll().stream()
                .map(quiz -> QuizResponse.de(quiz, perguntasDo(quiz)))
                .toList();
    }

    public List<ResultadoQuizResponse> listarRespostasDoUsuario(Usuario usuario) {
        return respostaQuizRepository.findAllByUsuarioId(usuario.getId()).stream()
                .map(ResultadoQuizResponse::de)
                .toList();
    }

    @Transactional
    public QuizResponse cadastrar(QuizRequest request, Usuario usuario) {
        if (!"ADMIN".equalsIgnoreCase(usuario.getPerfil()) && !"ADMINISTRADOR".equalsIgnoreCase(usuario.getPerfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente administradores podem cadastrar quizzes.");
        }
        if (request == null || request.titulo() == null || request.titulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É necessário informar o título do quiz.");
        }
        if (request.titulo().trim().length() > TAMANHO_MAXIMO_TEXTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O título pode ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }
        if (request.descricao() != null && request.descricao().trim().length() > TAMANHO_MAXIMO_TEXTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A descrição pode ter no máximo " + TAMANHO_MAXIMO_TEXTO + " caracteres.");
        }
        if (request.pontuacao() == null || request.pontuacao() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pontuação do quiz precisa ser maior que zero.");
        }
        if (request.pontuacao() > PONTUACAO_MAXIMA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A pontuação do quiz pode ser no máximo " + PONTUACAO_MAXIMA + ".");
        }
        if (request.palestraId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione a palestra do quiz.");
        }
        Palestra palestra = palestraRepository.findById(request.palestraId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Palestra não encontrada."));
        if (quizRepository.existsByPalestraIdAndTituloIgnoreCase(palestra.getId(), request.titulo().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um quiz com este título para a palestra.");
        }

        Quiz quiz = new Quiz();
        quiz.setTitulo(request.titulo().trim());
        quiz.setDescricao(request.descricao() == null ? null : request.descricao().trim());
        quiz.setPontuacao(request.pontuacao());
        quiz.setPalestra(palestra);

        List<Pergunta> perguntas = perguntasDo(quiz);
        if (perguntas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A palestra selecionada ainda não tem perguntas cadastradas.");
        }
        return QuizResponse.de(quizRepository.save(quiz), perguntas);
    }

    @Transactional
    public ResultadoQuizResponse responder(Long quizId, RespostaQuizRequest request, Usuario usuario) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Respostas inválidas.");
        }

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz não encontrado."));

        if (respostaQuizRepository.existsByUsuarioIdAndQuizId(usuario.getId(), quizId)) {
            throw jaRespondido();
        }

        Map<Integer, Set<Integer>> escolhas = new HashMap<>();
        if (request.respostas() != null) {
            for (RespostaQuizRequest.RespostaPergunta resposta : request.respostas()) {
                if (resposta.perguntaId() == null || resposta.alternativaIds() == null
                        || resposta.alternativaIds().isEmpty() || resposta.alternativaIds().contains(null)
                        || escolhas.put(resposta.perguntaId(), new HashSet<>(resposta.alternativaIds())) != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Respostas inválidas.");
                }
            }
        }

        List<Pergunta> perguntas = perguntasDo(quiz);
        int acertos = 0;
        int respondidas = 0;
        for (Pergunta pergunta : perguntas) {
            Set<Integer> escolhidas = escolhas.get(pergunta.getId());
            if (escolhidas == null) {
                continue;
            }
            respondidas++;
            List<Alternativa> alternativas = QuizResponse.alternativasDe(pergunta);
            Set<Integer> idsDaPergunta = alternativas.stream().map(Alternativa::getId).collect(Collectors.toSet());
            if (!idsDaPergunta.containsAll(escolhidas)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A alternativa escolhida não pertence à pergunta.");
            }
            // Só conta o acerto quem marcou exatamente as alternativas corretas, nem mais nem menos
            Set<Integer> corretas = alternativas.stream()
                    .filter(Alternativa::getCorreta)
                    .map(Alternativa::getId)
                    .collect(Collectors.toSet());
            if (corretas.equals(escolhidas)) {
                acertos++;
            }
        }
        if (respondidas != escolhas.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Há respostas para perguntas que não são deste quiz.");
        }

        // Pontuação proporcional aos acertos; acertando tudo o usuário recebe a pontuação cheia do quiz
        int pontos = perguntas.isEmpty() ? 0 : quiz.getPontuacao() * acertos / perguntas.size();

        RespostaQuiz resposta = new RespostaQuiz();
        resposta.setUsuarioId(usuario.getId());
        resposta.setQuizId(quizId);
        resposta.setAcertos(acertos);
        resposta.setTotalPerguntas(perguntas.size());
        resposta.setPontosObtidos(pontos);
        resposta.setDataResposta(LocalDateTime.now());
        try {
            respostaQuizRepository.saveAndFlush(resposta);
        } catch (DataIntegrityViolationException e) {
            // Dois envios ao mesmo tempo: o segundo esbarra na restrição única (usuario_id, quiz_id)
            throw jaRespondido();
        }

        if (pontos > 0) {
            rankingService.adicionarPontos(new PontuacaoRankingRequest(usuario.getId(), pontos, "QUIZ:" + quizId));
        }

        return ResultadoQuizResponse.de(resposta);
    }

    private List<Pergunta> perguntasDo(Quiz quiz) {
        return quiz.getPalestra().getPerguntas().stream()
                .filter(Pergunta::getAtivo)
                .toList();
    }

    private ResponseStatusException jaRespondido() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Você já respondeu este quiz.");
    }
}
