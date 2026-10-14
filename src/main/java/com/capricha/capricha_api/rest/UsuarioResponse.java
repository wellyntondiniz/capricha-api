package com.capricha.capricha_api.rest;

import com.capricha.capricha_api.entidade.Usuario;

// Dados do usuário enviados ao cliente. Não inclui a senha nem o hash dela.
public record UsuarioResponse(Integer id, String nome, String email, boolean ativo, String perfil) {

	public UsuarioResponse(Integer id, String nome, String email, boolean ativo) {
		this(id, nome, email, ativo, "PARTICIPANTE");
	}

	public static UsuarioResponse de(Usuario usuario) {
		String perfil = usuario.getPerfil() != null ? usuario.getPerfil() : "PARTICIPANTE";
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getAtivo(), perfil);
	}
}
