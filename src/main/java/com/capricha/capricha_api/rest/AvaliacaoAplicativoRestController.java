package com.capricha.capricha_api.rest;

import java.util.List;

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
import com.capricha.capricha_api.service.AvaliacaoAplicativoService.RespostaRecebida;
import com.capricha.capricha_api.service.SessaoUsuarioService;

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

    @GetMapping("/perguntas")
    public List<PerguntaResponse> perguntas() {
        return avaliacoes.listarPerguntas().stream()
                .map(p -> new PerguntaResponse(p.getId(), p.getEnunciado()))
                .toList();
    }

    @GetMapping("/minha")
    public StatusResponse minha(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return new StatusResponse(avaliacoes.jaRespondeu(sessoes.autenticar(authorization)));
    }

    @PostMapping("/respostas")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse responder(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody EnvioRequest envio) {
        avaliacoes.responder(sessoes.autenticar(authorization),
                envio == null ? null : envio.respostas());
        return new MensagemResponse("Avaliação enviada com sucesso. Obrigado pela sua opinião!");
    }

    public record PerguntaResponse(Integer id, String texto) {}
    public record StatusResponse(boolean respondida) {}
    public record EnvioRequest(List<RespostaRecebida> respostas) {}
    public record MensagemResponse(String mensagem) {}
}
