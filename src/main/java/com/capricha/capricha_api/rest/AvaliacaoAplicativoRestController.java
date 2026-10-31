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
                request.peou01(), request.peou02(), request.peou03(), request.peou04(), request.peou05(), request.peou06(),
                request.pu01(), request.pu02(), request.pu03(), request.pu04(), request.pu05(), request.pu06(),
                request.ac01(), request.ac02(), request.ac03(), request.ac04(), request.ac05(), request.ac06(), request.ac07(), request.ac08(),
                request.se01(), request.se02(), request.se03(), request.se04(), request.se05(), request.se06(), request.se07(), request.se08(),
                request.com01(), request.com02(), request.com03(),
                request.coo01(), request.coo02(), request.coo03(),
                request.cop01(), request.cop02(), request.cop03(),
                request.comentario()));
        return new MensagemResponse("Avaliação enviada com sucesso. Obrigado pela sua opinião!");
    }

    public record AvaliacaoRequest(
            @NotNull @Min(1) @Max(5) Integer peou01,
            @NotNull @Min(1) @Max(5) Integer peou02,
            @NotNull @Min(1) @Max(5) Integer peou03,
            @NotNull @Min(1) @Max(5) Integer peou04,
            @NotNull @Min(1) @Max(5) Integer peou05,
            @NotNull @Min(1) @Max(5) Integer peou06,
            @NotNull @Min(1) @Max(5) Integer pu01,
            @NotNull @Min(1) @Max(5) Integer pu02,
            @NotNull @Min(1) @Max(5) Integer pu03,
            @NotNull @Min(1) @Max(5) Integer pu04,
            @NotNull @Min(1) @Max(5) Integer pu05,
            @NotNull @Min(1) @Max(5) Integer pu06,
            @NotNull @Min(1) @Max(5) Integer ac01,
            @NotNull @Min(1) @Max(5) Integer ac02,
            @NotNull @Min(1) @Max(5) Integer ac03,
            @NotNull @Min(1) @Max(5) Integer ac04,
            @NotNull @Min(1) @Max(5) Integer ac05,
            @NotNull @Min(1) @Max(5) Integer ac06,
            @NotNull @Min(1) @Max(5) Integer ac07,
            @NotNull @Min(1) @Max(5) Integer ac08,
            @NotNull @Min(1) @Max(5) Integer se01,
            @NotNull @Min(1) @Max(5) Integer se02,
            @NotNull @Min(1) @Max(5) Integer se03,
            @NotNull @Min(1) @Max(5) Integer se04,
            @NotNull @Min(1) @Max(5) Integer se05,
            @NotNull @Min(1) @Max(5) Integer se06,
            @NotNull @Min(1) @Max(5) Integer se07,
            @NotNull @Min(1) @Max(5) Integer se08,
            @NotNull @Min(1) @Max(5) Integer com01,
            @NotNull @Min(1) @Max(5) Integer com02,
            @NotNull @Min(1) @Max(5) Integer com03,
            @NotNull @Min(1) @Max(5) Integer coo01,
            @NotNull @Min(1) @Max(5) Integer coo02,
            @NotNull @Min(1) @Max(5) Integer coo03,
            @NotNull @Min(1) @Max(5) Integer cop01,
            @NotNull @Min(1) @Max(5) Integer cop02,
            @NotNull @Min(1) @Max(5) Integer cop03,
            @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres.")
            String comentario) {}

    public record StatusResponse(boolean respondida) {}
    public record MensagemResponse(String mensagem) {}
}
