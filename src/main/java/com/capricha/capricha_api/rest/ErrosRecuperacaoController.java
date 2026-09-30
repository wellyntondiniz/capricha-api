package com.capricha.capricha_api.rest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice(assignableTypes = RecuperacaoEmailCodigoController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrosRecuperacaoController {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validar(MethodArgumentNotValidException erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem", mensagemDoCampo(erro.getBindingResult().getFieldError())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> corpoInvalido(HttpMessageNotReadableException erro) {
        return ResponseEntity.badRequest().body(Map.of("mensagem",
            "Não foi possível ler os dados enviados. Confira as informações e tente novamente."));
    }

    private String mensagemDoCampo(FieldError campo) {
        if (campo == null) return "Confira os dados informados e tente novamente.";
        return switch (campo.getField()) {
            case "email" -> "NotBlank".equals(campo.getCode())
                ? "Informe seu e-mail."
                : "Informe um e-mail válido, por exemplo: voce@exemplo.com.";
            case "codigo" -> "Digite o código de 6 números.";
            case "novaSenha" -> "Use entre 8 e 128 caracteres na senha.";
            case "desafio" -> "Solicitação inválida ou expirada. Peça um novo código.";
            default -> "Confira os dados informados e tente novamente.";
        };
    }
}