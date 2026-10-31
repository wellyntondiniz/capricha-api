package com.capricha.capricha_api.rest;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.service.ConfirmacaoCadastroService.Resultado;

// Resposta do cadastro: dados do usuário (sem senha) + situação do e-mail de confirmação.
public record CadastroUsuarioResponse(Integer id, String nome, String email, boolean ativo, String perfil,
		String foto, boolean emailConfirmacaoEnviado, boolean emailConfirmacaoSimulado, String mensagemEmail) {

	public static CadastroUsuarioResponse de(Usuario usuario, Resultado email) {
		UsuarioResponse base = UsuarioResponse.de(usuario);
		return new CadastroUsuarioResponse(base.id(), base.nome(), base.email(), base.ativo(), base.perfil(),
				base.foto(), email.enviado(), email.simulado(), email.mensagem());
	}
}