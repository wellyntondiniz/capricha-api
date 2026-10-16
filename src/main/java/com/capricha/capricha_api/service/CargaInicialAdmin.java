package com.capricha.capricha_api.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.UsuarioRepository;

@Component
@Order(1)
public class CargaInicialAdmin implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(CargaInicialAdmin.class);

	private final UsuarioRepository usuarioRepository;

	@Value("${app.admin.nome:Administrador}")
	private String adminNome;

	@Value("${app.admin.email:admin@capricha.com}")
	private String adminEmail;

	@Value("${app.admin.senha:Admin@123}")
	private String adminSenha;

	public CargaInicialAdmin(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (adminEmail == null || adminEmail.isBlank()) {
			log.info("Carga inicial de administrador desativada (email não configurado).");
			return;
		}

		String emailNormalizado = adminEmail.trim().toLowerCase();
		Optional<Usuario> existente = usuarioRepository.findFirstByEmailIgnoreCase(emailNormalizado);

		if (existente.isEmpty()) {
			Usuario admin = new Usuario();
			admin.setNome(adminNome != null && !adminNome.isBlank() ? adminNome.trim() : "Administrador");
			admin.setEmail(emailNormalizado);
			admin.setSenha(Senhas.proteger(adminSenha));
			admin.setPerfil("ADMIN");
			admin.setAtivo(true);
			usuarioRepository.save(admin);
			log.info("Usuário administrador inicial criado com sucesso: {}", emailNormalizado);
		} else {
			Usuario admin = existente.get();
			boolean alterado = false;

			if (!admin.getAtivo()) {
				admin.setAtivo(true);
				alterado = true;
			}

			if (!"ADMIN".equalsIgnoreCase(admin.getPerfil())) {
				admin.setPerfil("ADMIN");
				alterado = true;
			}

			if (alterado) {
				usuarioRepository.save(admin);
				log.info("Usuário existente atualizado com perfil ADMIN e ativo: {}", emailNormalizado);
			} else {
				log.info("Usuário administrador já existente e ativo: {}", emailNormalizado);
			}
		}
	}
}
