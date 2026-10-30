package com.capricha.capricha_api.dto;

import com.capricha.capricha_api.model.RespostaQuiz;

public record ResultadoQuizResponse(Long quizId, Integer acertos, Integer totalPerguntas, Integer pontosObtidos) {

    public static ResultadoQuizResponse de(RespostaQuiz resposta) {
        return new ResultadoQuizResponse(resposta.getQuizId(), resposta.getAcertos(),
                resposta.getTotalPerguntas(), resposta.getPontosObtidos());
    }
}
