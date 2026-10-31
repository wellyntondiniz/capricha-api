package com.capricha.capricha_api.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.capricha.capricha_api.service.QuizService;
import com.capricha.capricha_api.service.SessaoUsuarioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Todos os endpoints exigem sessão; quem responde é sempre o usuário da sessão, nunca um id enviado no corpo. */
@RestController
@RequestMapping("/quizzes")
@CrossOrigin
public class QuizRestController {

	private final QuizService quizzes;
	private final SessaoUsuarioService sessoes;

	public QuizRestController(QuizService quizzes, SessaoUsuarioService sessoes) {
		this.quizzes = quizzes;
		this.sessoes = sessoes;
	}

	public record PedidoResposta(
			@NotNull(message = "Responda todas as perguntas antes de enviar.") List<QuizService.RespostaPergunta> respostas) {}

	@GetMapping
	public List<QuizService.QuizDto> listar(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return quizzes.listar(sessoes.autenticar(authorization));
	}

	@GetMapping("/respostas")
	public List<QuizService.ResultadoDto> minhasRespostas(
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return quizzes.minhasRespostas(sessoes.autenticar(authorization));
	}

	@PostMapping("/{id}/respostas")
	@ResponseStatus(HttpStatus.CREATED)
	public QuizService.ResultadoDto responder(@PathVariable Long id, @Valid @RequestBody PedidoResposta pedido,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return quizzes.responder(id, sessoes.autenticar(authorization), pedido.respostas());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public QuizService.QuizDto cadastrar(@RequestBody QuizService.NovoQuiz quiz,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return quizzes.criar(sessoes.autenticar(authorization), quiz);
	}

	@PutMapping("/{id}")
	public QuizService.QuizDto atualizar(@PathVariable Long id, @RequestBody QuizService.EdicaoQuiz quiz,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return quizzes.atualizar(sessoes.autenticar(authorization), id, quiz);
	}
}