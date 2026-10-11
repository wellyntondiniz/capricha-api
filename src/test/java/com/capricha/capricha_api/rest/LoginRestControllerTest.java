package com.capricha.capricha_api.rest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.service.Senhas;
import com.capricha.capricha_api.service.UsuarioService;

class LoginRestControllerTest {

	private UsuarioService usuarioService;
	private MockMvc mockMvc;

	@BeforeEach
	void configurar() {
		usuarioService = mock(UsuarioService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new LoginRestController(usuarioService))
				.setControllerAdvice(new ErrosController())
				.build();
	}

	@Test
	void loginValidoNaoDevolveSenhaNemHash() throws Exception {
		Usuario usuario = new Usuario();
		usuario.setId(1);
		usuario.setNome("joao");
		usuario.setEmail("joao@exemplo.com");
		usuario.setSenha(Senhas.proteger("senha1234"));
		when(usuarioService.autenticar("joao@exemplo.com", "senha1234")).thenReturn(usuario);

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"joao@exemplo.com\",\"senha\":\"senha1234\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("joao@exemplo.com"))
				.andExpect(jsonPath("$.senha").doesNotExist());
	}

	@Test
	void credenciaisErradasDevolvem401() throws Exception {
		when(usuarioService.autenticar(anyString(), anyString()))
				.thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."));

		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"joao@exemplo.com\",\"senha\":\"errada123\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.mensagem").value("E-mail ou senha inválidos."));
	}

	@Test
	void camposVaziosDevolvemMensagemPorCampo() throws Exception {
		mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\",\"senha\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.campos.email").value("Informe o e-mail."))
				.andExpect(jsonPath("$.campos.senha").value("Informe a senha."));
	}
}
