package com.capricha.capricha_api.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.FieldError;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrosController {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> tratar(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(Map.of("mensagem",
            erro.getReason() == null ? "Não foi possível concluir a solicitação." : erro.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validar(MethodArgumentNotValidException erro) {
        // Uma mensagem por campo. Se o campo está vazio, "obrigatório" prevalece sobre as demais regras.
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError campo : erro.getBindingResult().getFieldErrors()) {
            if ("NotBlank".equals(campo.getCode()) || "NotNull".equals(campo.getCode())) {
                campos.put(campo.getField(), campo.getDefaultMessage());
            } else {
                campos.putIfAbsent(campo.getField(), campo.getDefaultMessage());
            }
        }
        String mensagem = campos.isEmpty()
            ? "Dados inválidos. Confira os campos informados."
            : campos.values().iterator().next();
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("mensagem", mensagem);
        corpo.put("campos", campos);
        return ResponseEntity.badRequest().body(corpo);
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicado(DataIntegrityViolationException erro) {
        return ResponseEntity.status(409).body(Map.of("mensagem", "Cadastro duplicado ou dados inválidos. Confira o e-mail informado."));
    }
}
