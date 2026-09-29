package com.capricha.capricha_api.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.service.UsuarioService;

class CadastroUsuarioValidacaoTest {

	private UsuarioService usuarioService;
	private MockMvc mockMvc;

	@BeforeEach
	void configurar() {
		usuarioService = mock(UsuarioService.class);
		UsuarioRestController controller = new UsuarioRestController();
		ReflectionTestUtils.setField(controller, "usuarioService", usuarioService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller)
				.setControllerAdvice(new ErrosController())
				.build();
	}

	private void cadastrar(String json, int statusEsperado,
			org.springframework.test.web.servlet.ResultMatcher... verificacoes) throws Exception {
		var resultado = mockMvc.perform(post("/usuario")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json))
				.andExpect(status().is(statusEsperado));
		for (var verificacao : verificacoes) {
			resultado.andExpect(verificacao);
		}
	}

	@Test
	void informaCadaCampoObrigatorioVazio() throws Exception {
		cadastrar("{\"nome\":\"  \",\"email\":\"\",\"senha\":\"\"}", 400,
				jsonPath("$.campos.nome").value("Informe o nome de usuário."),
				jsonPath("$.campos.email").value("Informe o e-mail."),
				jsonPath("$.campos.senha").value("Informe a senha."),
				jsonPath("$.mensagem").exists());
		verify(usuarioService, never()).cadastrarUsuario(any());
	}

	@Test
	void informaFormatosInvalidos() throws Exception {
		cadastrar("{\"nome\":\"joao\",\"email\":\"joao@exemplo\",\"senha\":\"123\"}", 400,
				jsonPath("$.campos.email").value("Digite um e-mail válido (ex: voce@exemplo.com)."),
				jsonPath("$.campos.senha").value("A senha deve ter entre 8 e 128 caracteres."),
				jsonPath("$.campos.nome").doesNotExist());
		verify(usuarioService, never()).cadastrarUsuario(any());
	}

	@Test
	void recusaSenhaComEspacoNasPontas() throws Exception {
		cadastrar("{\"nome\":\"joao\",\"email\":\"joao@exemplo.com\",\"senha\":\" senha1234\"}", 400,
				jsonPath("$.campos.senha").value("A senha não pode começar ou terminar com espaço."));
	}

	@Test
	void cadastraQuandoTodosOsCamposSaoValidos() throws Exception {
		when(usuarioService.cadastrarUsuario(any())).thenAnswer(chamada -> chamada.getArgument(0, Usuario.class));
		cadastrar("{\"nome\":\" joao \",\"email\":\" joao@exemplo.com \",\"senha\":\"senha1234\"}", 200,
				jsonPath("$.nome").value("joao"),
				jsonPath("$.email").value("joao@exemplo.com"));
	}
}
