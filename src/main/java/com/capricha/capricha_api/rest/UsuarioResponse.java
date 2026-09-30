package com.capricha.capricha_api.rest;

import com.capricha.capricha_api.entidade.Usuario;

// Dados do usuário enviados ao cliente. Não inclui a senha nem o hash dela.
public record UsuarioResponse(Integer id, String nome, String email, boolean ativo) {

	public static UsuarioResponse de(Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getAtivo());
	}
}
