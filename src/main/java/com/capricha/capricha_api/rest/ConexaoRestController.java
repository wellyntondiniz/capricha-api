package com.capricha.capricha_api.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.capricha.capricha_api.service.ConexaoService;
import com.capricha.capricha_api.service.SessaoUsuarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


@RestController
@RequestMapping("/conexoes")
@CrossOrigin
public class ConexaoRestController {
    private final ConexaoService conexoes;
    private final SessaoUsuarioService sessoes;

    public ConexaoRestController(ConexaoService conexoes, SessaoUsuarioService sessoes) {
        this.conexoes = conexoes;
        this.sessoes = sessoes;
    }

    public record Pedido(@NotBlank @Size(max = 36) String qrToken, @NotNull @Positive Integer eventoId) {}

    @GetMapping("/meu-qr")
    public ConexaoService.MeuQr meuQr(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return conexoes.meuQr(sessoes.autenticar(authorization));
    }

    @GetMapping
    public ConexaoService.Resumo resumo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return conexoes.resumo(sessoes.autenticar(authorization));
    }

    @PostMapping("/identificar")
    public ConexaoService.Perfil identificar(@Valid @RequestBody Pedido pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return conexoes.identificar(pedido.eventoId(), pedido.qrToken(), sessoes.autenticar(authorization));
    }

    @PostMapping("/solicitacoes")
    @ResponseStatus(HttpStatus.CREATED)
    public ConexaoService.Solicitacao solicitar(@Valid @RequestBody Pedido pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return conexoes.solicitar(pedido.eventoId(), pedido.qrToken(), sessoes.autenticar(authorization));
    }

    @PostMapping("/solicitacoes/{id}/aceitar")
    public ConexaoService.Resultado aceitar(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return conexoes.aceitar(id, sessoes.autenticar(authorization));
    }

    @PostMapping("/solicitacoes/{id}/recusar")
    public ConexaoService.Resultado recusar(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return conexoes.recusar(id, sessoes.autenticar(authorization));
    }
}