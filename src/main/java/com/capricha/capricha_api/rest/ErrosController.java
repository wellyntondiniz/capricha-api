package com.capricha.capricha_api.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ErrosController {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratar(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("mensagem",
            erro.getReason() == null ? "Não foi possível concluir a solicitação." : erro.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validar(MethodArgumentNotValidException erro) {
        Set<String> campos = erro.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getField).collect(Collectors.toSet());
        String mensagem;
        if (erro.getBindingResult().getTarget() instanceof RecuperacaoEmailCodigoController.Solicitar) {
            mensagem = "Informe um e-mail válido.";
        } else if (campos.contains("codigo")) {
            mensagem = "Código inválido. Digite os 6 números do código recebido.";
        } else if (campos.contains("novaSenha")) {
            mensagem = "A nova senha deve ter entre 8 e 128 caracteres e não pode ter apenas espaços.";
        } else if (campos.contains("desafio")) {
            mensagem = "Solicitação de recuperação inválida. Solicite um novo código.";
        } else {
            mensagem = "Confira os campos: nome, e-mail, celular com DDI e DDD e senha entre 8 e 128 caracteres.";
        }
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagem));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicado(DataIntegrityViolationException erro) {
        return ResponseEntity.status(409).body(Map.of("mensagem", "Cadastro duplicado ou dados inválidos. Confira o e-mail informado."));
    }
}