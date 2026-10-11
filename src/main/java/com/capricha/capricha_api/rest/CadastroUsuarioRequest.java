package com.capricha.capricha_api.rest;

import com.capricha.capricha_api.entidade.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Dados recebidos no cadastro. As validações ficam aqui (e não na entidade)
// para não afetar outros fluxos que salvam o Usuario, como a recuperação de senha.
public record CadastroUsuarioRequest(
		@NotBlank(message = "Informe o nome de usuário.")
		@Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
		String nome,

		@NotBlank(message = "Informe o e-mail.")
		@Email(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$", message = "Digite um e-mail válido (ex: voce@exemplo.com).")
		@Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
		String email,

		@NotBlank(message = "Informe a senha.")
		@Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.")
		@Pattern(regexp = "^\\S(.*\\S)?$", message = "A senha não pode começar ou terminar com espaço.")
		String senha) {

	// Remove espaços acidentais antes da validação (a senha é mantida como digitada).
	public CadastroUsuarioRequest {
		nome = nome == null ? null : nome.trim();
		email = email == null ? null : email.trim();
	}

	public Usuario paraUsuario() {
		Usuario usuario = new Usuario();
		usuario.setNome(nome);
		usuario.setEmail(email);
		usuario.setSenha(senha);
		usuario.setAtivo(true);
		return usuario;
	}
}
