package com.capricha.capricha_api.controller;

import com.capricha.capricha_api.dto.QuizRequest;
import com.capricha.capricha_api.dto.QuizResponse;
import com.capricha.capricha_api.dto.RespostaQuizRequest;
import com.capricha.capricha_api.dto.ResultadoQuizResponse;
import com.capricha.capricha_api.service.QuizService;
import com.capricha.capricha_api.service.SessaoUsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quizzes")
@CrossOrigin(origins = "*")
public class QuizController {

    private final QuizService quizService;
    private final SessaoUsuarioService sessoes;

    public QuizController(QuizService quizService, SessaoUsuarioService sessoes) {
        this.quizService = quizService;
        this.sessoes = sessoes;
    }

    // Cadastro feito pelo administrador
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizResponse cadastrar(@RequestBody QuizRequest quiz,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return quizService.cadastrar(quiz, sessoes.autenticar(authorization));
    }

    @GetMapping
    public List<QuizResponse> listar() {
        return quizService.listar();
    }

    // Quizzes já respondidos pelo usuário logado
    @GetMapping("/respostas")
    public List<ResultadoQuizResponse> listarRespostas(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return quizService.listarRespostasDoUsuario(sessoes.autenticar(authorization));
    }

    @PostMapping("/{id}/respostas")
    public ResultadoQuizResponse responder(@PathVariable Long id, @RequestBody RespostaQuizRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return quizService.responder(id, request, sessoes.autenticar(authorization));
    }
}
