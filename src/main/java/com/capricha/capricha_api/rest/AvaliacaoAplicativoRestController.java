package com.capricha.capricha_api.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.capricha.capricha_api.service.AvaliacaoAplicativoService;
import com.capricha.capricha_api.service.AvaliacaoAplicativoService.Respostas;
import com.capricha.capricha_api.service.SessaoUsuarioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/avaliacoes-aplicativo")
@CrossOrigin
public class AvaliacaoAplicativoRestController {
    private final AvaliacaoAplicativoService avaliacoes;
    private final SessaoUsuarioService sessoes;

    public AvaliacaoAplicativoRestController(AvaliacaoAplicativoService avaliacoes,
            SessaoUsuarioService sessoes) {
        this.avaliacoes = avaliacoes;
        this.sessoes = sessoes;
    }

    @GetMapping("/minha")
    public StatusResponse minha(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return new StatusResponse(avaliacoes.jaRespondeu(sessoes.autenticar(authorization)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse responder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AvaliacaoRequest request) {
        avaliacoes.salvar(sessoes.autenticar(authorization), new Respostas(
                request.navegacao(), request.clarezaInformacoes(),
                request.utilidadeFuncionalidades(), request.desempenhoTecnico(),
                request.recomendacao(), request.comentario()));
        return new MensagemResponse("Avaliação enviada com sucesso. Obrigado pela sua opinião!");
    }

    public record AvaliacaoRequest(
            @NotNull @Min(1) @Max(5) Integer navegacao,
            @NotNull @Min(1) @Max(5) Integer clarezaInformacoes,
            @NotNull @Min(1) @Max(5) Integer utilidadeFuncionalidades,
            @NotNull @Min(1) @Max(5) Integer desempenhoTecnico,
            @NotNull @Min(1) @Max(5) Integer recomendacao,
            @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.")
            String comentario) {}

    public record StatusResponse(boolean respondida) {}
    public record MensagemResponse(String mensagem) {}
}
