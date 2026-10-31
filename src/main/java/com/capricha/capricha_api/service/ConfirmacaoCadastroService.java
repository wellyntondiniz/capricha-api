package com.capricha.capricha_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.capricha.capricha_api.entidade.Usuario;

// Envia o e-mail de confirmação DEPOIS que o cadastro já foi gravado.
// Uma falha no envio nunca desfaz o cadastro: o resultado é informado ao chamador.
@Service
public class ConfirmacaoCadastroService {

	private static final Logger log = LoggerFactory.getLogger(ConfirmacaoCadastroService.class);

	public record Resultado(boolean enviado, boolean simulado, String mensagem) {}

	private final EnvioCodigoEmail envio;

	public ConfirmacaoCadastroService(EnvioCodigoEmail envio) {
		this.envio = envio;
	}

	public Resultado enviar(Usuario usuario) {
		try {
			envio.enviarConfirmacaoCadastro(usuario.getEmail(), usuario.getNome());
		} catch (Exception erro) {
			log.warn("Cadastro {} concluído, mas o e-mail de confirmação falhou: {}", usuario.getId(), erro.getMessage());
			return new Resultado(false, false,
				"Cadastro concluído, mas não foi possível enviar o e-mail de confirmação para "
				+ usuario.getEmail() + ". Você já pode entrar com sua conta.");
		}
		if (envio.simulado()) {
			return new Resultado(false, true,
				"Cadastro concluído. Modo local: o e-mail de confirmação foi simulado no terminal da API.");
		}
		return new Resultado(true, false,
			"Cadastro concluído! Enviamos um e-mail de confirmação para " + usuario.getEmail() + ".");
	}
}