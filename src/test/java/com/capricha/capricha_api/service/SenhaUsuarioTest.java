package com.capricha.capricha_api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.UsuarioRepository;

class SenhaUsuarioTest {

	private UsuarioRepository repositorio;
	private UsuarioService service;

	@BeforeEach
	void configurar() {
		repositorio = mock(UsuarioRepository.class);
		service = new UsuarioService();
		ReflectionTestUtils.setField(service, "usuarioRepository", repositorio);
		when(repositorio.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
	}

	private static Usuario usuario(String email, String senhaArmazenada) {
		Usuario usuario = new Usuario();
		usuario.setNome("joao");
		usuario.setEmail(email);
		usuario.setSenha(senhaArmazenada);
		return usuario;
	}

	@Test
	void hashNaoContemASenhaEUsaSaltDiferenteACadaVez() {
		String primeiro = Senhas.proteger("senha1234");
		String segundo = Senhas.proteger("senha1234");

		assertThat(primeiro).startsWith(Senhas.PREFIXO).doesNotContain("senha1234");
		assertThat(primeiro).isNotEqualTo(segundo);
		assertThat(Senhas.confere("senha1234", primeiro)).isTrue();
		assertThat(Senhas.confere("outraSenha", primeiro)).isFalse();
	}

	@Test
	void naoAceitaSenhaArmazenadaEmTextoPuro() {
		assertThat(Senhas.confere("senha1234", "senha1234")).isFalse();
	}

	@Test
	void cadastroGravaSomenteOHash() {
		Usuario salvo = service.cadastrarUsuario(usuario("joao@exemplo.com", "senha1234"));

		assertThat(salvo.getSenha()).startsWith(Senhas.PREFIXO).isNotEqualTo("senha1234");
		assertThat(Senhas.confere("senha1234", salvo.getSenha())).isTrue();
	}

	@Test
	void loginConfereASenhaComOHashArmazenado() {
		Usuario armazenado = usuario("joao@exemplo.com", Senhas.proteger("senha1234"));
		when(repositorio.findFirstByEmailIgnoreCaseAndAtivoTrue("joao@exemplo.com")).thenReturn(Optional.of(armazenado));

		assertThat(service.autenticar(" joao@exemplo.com ", "senha1234")).isSameAs(armazenado);
	}

	@Test
	void loginComSenhaErradaOuEmailInexistenteDaAMesmaMensagem() {
		Usuario armazenado = usuario("joao@exemplo.com", Senhas.proteger("senha1234"));
		when(repositorio.findFirstByEmailIgnoreCaseAndAtivoTrue(anyString())).thenReturn(Optional.empty());
		when(repositorio.findFirstByEmailIgnoreCaseAndAtivoTrue("joao@exemplo.com")).thenReturn(Optional.of(armazenado));

		assertThatThrownBy(() -> service.autenticar("joao@exemplo.com", "errada123"))
				.isInstanceOfSatisfying(ResponseStatusException.class, erro -> {
					assertThat(erro.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					assertThat(erro.getReason()).isEqualTo("E-mail ou senha inválidos.");
				});
		assertThatThrownBy(() -> service.autenticar("naoexiste@exemplo.com", "senha1234"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						erro -> assertThat(erro.getReason()).isEqualTo("E-mail ou senha inválidos."));
	}

	@Test
	void migracaoConverteSenhasAntigasEmTextoPuro() {
		Usuario antigo = usuario("antigo@exemplo.com", "senhaAntiga1");
		when(repositorio.findBySenhaNotLike(Senhas.PREFIXO + "%")).thenReturn(List.of(antigo));

		new MigracaoSenhasTextoPuro(repositorio).run(null);

		assertThat(antigo.getSenha()).startsWith(Senhas.PREFIXO);
		assertThat(Senhas.confere("senhaAntiga1", antigo.getSenha())).isTrue();
		verify(repositorio).saveAll(List.of(antigo));
	}

	@Test
	void migracaoNaoFazNadaQuandoTudoJaEstaEmHash() {
		when(repositorio.findBySenhaNotLike(Senhas.PREFIXO + "%")).thenReturn(List.of());

		new MigracaoSenhasTextoPuro(repositorio).run(null);

		verify(repositorio, never()).saveAll(any());
	}
}
