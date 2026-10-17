package com.capricha.capricha_api.rest;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capricha.capricha_api.service.UsuarioService;
import com.capricha.capricha_api.service.SessaoUsuarioService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class LoginRestController {

	private final UsuarioService usuarioService;
	private final SessaoUsuarioService sessoes;

	public LoginRestController(UsuarioService usuarioService, SessaoUsuarioService sessoes) {
		this.usuarioService = usuarioService;
		this.sessoes = sessoes;
	}

	// Confere a senha informada com o hash armazenado. Responde 401 se e-mail ou senha estiverem errados.
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest login) {
		var usuario = usuarioService.autenticar(login.email(), login.senha());
		return LoginResponse.de(usuario, sessoes.emitir(usuario));
	}

	@GetMapping("/sessao")
	public UsuarioResponse sessao(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return UsuarioResponse.de(sessoes.autenticar(authorization));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
		sessoes.encerrar(authorization);
	}

	public record LoginRequest(
			@NotBlank(message = "Informe o e-mail.")
			@Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Digite um e-mail válido (ex: voce@exemplo.com).")
			@Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
			String email,

			@NotBlank(message = "Informe a senha.")
			@Size(max = 128, message = "A senha deve ter no máximo 128 caracteres.")
			String senha) {

		public LoginRequest {
			email = email == null ? null : email.trim();
		}
	}
}
