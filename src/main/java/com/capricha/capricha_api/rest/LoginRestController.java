package com.capricha.capricha_api.rest;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.capricha.capricha_api.service.UsuarioService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/auth")
@CrossOrigin
public class LoginRestController {

	private final UsuarioService usuarioService;

	public LoginRestController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	// Confere a senha informada com o hash armazenado. Responde 401 se e-mail ou senha estiverem errados.
	@PostMapping("/login")
	public UsuarioResponse login(@Valid @RequestBody LoginRequest login) {
		return UsuarioResponse.de(usuarioService.autenticar(login.email(), login.senha()));
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
