package com.capricha.capricha_api.dto;

import java.util.List;

// O usuário vem da sessão (cabeçalho Authorization), nunca do corpo da requisição
public record RespostaQuizRequest(List<RespostaPergunta> respostas) {

    // Uma pergunta pode ter mais de uma alternativa correta: o participante marca todas as que achar corretas
    public record RespostaPergunta(Integer perguntaId, List<Integer> alternativaIds) {
    }
}
