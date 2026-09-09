package com.capricha.capricha_api.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;

@RestControllerAdvice
public class ErrosController {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratar(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("mensagem",
            erro.getReason() == null ? "Não foi possível concluir a solicitação." : erro.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validar(MethodArgumentNotValidException erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", "Confira os campos: nome, e-mail, celular com DDI e DDD e senha entre 8 e 128 caracteres."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicado(DataIntegrityViolationException erro) {
        return ResponseEntity.status(409).body(Map.of("mensagem", "Cadastro duplicado ou dados inválidos. Confira o e-mail informado."));
    }
}
