package com.ca06.api.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApiException(ApiException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiError(exception.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String detail = exception.getMostSpecificCause().getMessage();
        if (detail != null && detail.contains("uk_event_checkin_user_event")) {
            return ResponseEntity.status(409).body(new ApiError(
                    "ALREADY_CHECKED_IN",
                    "Você já realizou check-in neste evento."));
        }
        return ResponseEntity.internalServerError().body(new ApiError(
                "DATABASE_ERROR",
                "Não foi possível concluir a operação no banco de dados."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "INVALID_REQUEST",
                "Dados obrigatórios não informados ou inválidos."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                "INVALID_REQUEST",
                "O corpo da requisição está inválido."));
    }
}
