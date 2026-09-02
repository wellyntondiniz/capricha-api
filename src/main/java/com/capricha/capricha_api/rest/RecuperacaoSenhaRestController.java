package com.capricha.capricha_api.rest;

import com.capricha.capricha_api.service.RecuperacaoSenhaService;
import com.capricha.capricha_api.service.RecuperacaoSenhaService.ResultadoRedefinicao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class RecuperacaoSenhaRestController {
    private final RecuperacaoSenhaService service;

    public RecuperacaoSenhaRestController(RecuperacaoSenhaService service) { this.service = service; }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Map<String, String>> solicitar(@Valid @RequestBody SolicitarRequest request) {
        try {
            service.solicitar(request.email());
            return ResponseEntity.ok(Map.of("mensagem",
                    "Se o e-mail estiver cadastrado, o link será enviado."));
        } catch (MailException exception) {
            return ResponseEntity.status(503).body(Map.of("mensagem",
                    "Não foi possível enviar o e-mail. Verifique a configuração SMTP."));
        }
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinir(@Valid @RequestBody RedefinirRequest request) {
        ResultadoRedefinicao resultado = service.redefinir(request.token(), request.novaSenha());
        if (resultado == ResultadoRedefinicao.SENHA_REPETIDA) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensagem", "A nova senha não pode ser igual à senha anterior."));
        }
        if (resultado != ResultadoRedefinicao.SUCESSO) {
            return ResponseEntity.badRequest().body(Map.of(
                    "mensagem", "Token inválido, expirado ou usuário não encontrado."));
        }
        return ResponseEntity.ok(Map.of("mensagem", "Senha alterada com sucesso."));
    }

    public record SolicitarRequest(@NotBlank @Email String email) {}
    public record RedefinirRequest(@NotBlank String token,
            @NotBlank @Size(min = 8) String novaSenha) {}
}
