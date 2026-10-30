package com.capricha.capricha_api.dto;

public record QuizRequest(String titulo, String descricao, Integer pontuacao, Long palestraId) {
}
