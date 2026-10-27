package com.capricha.capricha_api.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.capricha.capricha_api.service.SessaoUsuarioService;
import com.capricha.capricha_api.service.VotoResumoService;

@RestController
@CrossOrigin
public class VotoResumoRestController {

	private final VotoResumoService votos;
	private final SessaoUsuarioService sessoes;

	public VotoResumoRestController(VotoResumoService votos, SessaoUsuarioService sessoes) {
		this.votos = votos;
		this.sessoes = sessoes;
	}

	@GetMapping("/evento/{id}/votacao")
	public VotoResumoService.Situacao situacao(@PathVariable Integer id,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return votos.situacao(id, sessoes.autenticar(authorization));
	}

	@PostMapping("/resumos/{id}/voto")
	@ResponseStatus(HttpStatus.CREATED)
	public VotoResumoService.Registro votar(@PathVariable Integer id,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return votos.votar(id, sessoes.autenticar(authorization));
	}

	@GetMapping("/resumos/{id}/qr-code")
	public VotoResumoService.QrCodeResumo qrCode(@PathVariable Integer id,
			@RequestHeader(value = "Authorization", required = false) String authorization) {
		return votos.gerarQr(id, sessoes.autenticar(authorization));
	}
}