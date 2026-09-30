package com.capricha.capricha_api.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.UsuarioRepository;

// Contas criadas antes do hash no cadastro têm a senha em texto puro no banco.
// Ao iniciar a API, converte essas senhas para hash. Depois da primeira execução, não encontra mais nenhuma.
@Component
public class MigracaoSenhasTextoPuro implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(MigracaoSenhasTextoPuro.class);

	private final UsuarioRepository usuarioRepository;

	public MigracaoSenhasTextoPuro(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		List<Usuario> pendentes = usuarioRepository.findBySenhaNotLike(Senhas.PREFIXO + "%");
		if (pendentes.isEmpty()) return;

		for (Usuario usuario : pendentes) {
			usuario.setSenha(Senhas.proteger(usuario.getSenha()));
		}
		usuarioRepository.saveAll(pendentes);
		log.info("Senhas em texto puro convertidas para hash: {} usuário(s).", pendentes.size());
	}
}
