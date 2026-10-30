package com.capricha.capricha_api.dto;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Pergunta;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.model.Quiz;

import java.util.List;

// Não expõe o campo "correta" das alternativas para não entregar o gabarito ao participante
public record QuizResponse(Long id, String titulo, String descricao, Integer pontuacao,
                           PalestraResumo palestra, List<PerguntaQuiz> perguntas) {

    public record PalestraResumo(Long id, String nome) {
    }

    public record PerguntaQuiz(Integer id, String enunciado, List<AlternativaQuiz> alternativas) {
    }

    public record AlternativaQuiz(Integer id, String texto) {
    }

    public static QuizResponse de(Quiz quiz, List<Pergunta> perguntas) {
        Palestra palestra = quiz.getPalestra();
        return new QuizResponse(quiz.getId(), quiz.getTitulo(), quiz.getDescricao(), quiz.getPontuacao(),
                new PalestraResumo(palestra.getId(), palestra.getNome()),
                perguntas.stream()
                        .map(pergunta -> new PerguntaQuiz(pergunta.getId(), pergunta.getEnunciado(),
                                alternativasDe(pergunta).stream()
                                        .map(alternativa -> new AlternativaQuiz(alternativa.getId(), alternativa.getTexto()))
                                        .toList()))
                        .toList());
    }

    public static List<Alternativa> alternativasDe(Pergunta pergunta) {
        return pergunta.getAlternativas() == null ? List.of() : pergunta.getAlternativas();
    }
}
