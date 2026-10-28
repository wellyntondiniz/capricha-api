package com.capricha.capricha_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.CodigoEmail;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.CodigoEmailRepository;
import com.capricha.capricha_api.repository.RecuperacaoUsuarioRepository;

class RecuperacaoEmailCodigoServiceTest {

	private CodigoEmailRepository codigos;
	private RecuperacaoUsuarioRepository usuarios;
	private EnvioCodigoEmail envio;
	private RecuperacaoEmailCodigoService service;

	@BeforeEach
	void configurar() {
		codigos = mock(CodigoEmailRepository.class);
		usuarios = mock(RecuperacaoUsuarioRepository.class);
		envio = mock(EnvioCodigoEmail.class);
		service = new RecuperacaoEmailCodigoService(codigos, usuarios, envio);
	}

	@Test
	void emailSemUsuarioNaoGeraDesafioNemEnviaCodigo() {
		when(usuarios.buscarAtivoParaAtualizacao("naoexiste@exemplo.com")).thenReturn(Optional.empty());

		var erro = assertThrows(ResponseStatusException.class, () -> service.solicitar("NaoExiste@Exemplo.com "));

		assertEquals(HttpStatus.NOT_FOUND, erro.getStatusCode());
		verify(codigos, never()).save(any());
		verify(envio, never()).enviar(anyString(), anyString(), anyString());
	}

	@Test
	void solicitacaoDentroDoIntervaloDeUmMinutoNaoDevolveDesafio() {
		when(usuarios.buscarAtivoParaAtualizacao("ana@exemplo.com")).thenReturn(Optional.of(new Usuario()));
		CodigoEmail recente = new CodigoEmail();
		recente.criado = Instant.now().minusSeconds(10);
		when(codigos.findFirstByEmailOrderByCriadoDesc("ana@exemplo.com")).thenReturn(Optional.of(recente));

		var erro = assertThrows(ResponseStatusException.class, () -> service.solicitar("ana@exemplo.com"));

		assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatusCode());
		verify(envio, never()).enviar(anyString(), anyString(), anyString());
	}

	@Test
	void limiteDeTresSolicitacoesEmQuinzeMinutosNaoDevolveDesafio() {
		when(usuarios.buscarAtivoParaAtualizacao("ana@exemplo.com")).thenReturn(Optional.of(new Usuario()));
		when(codigos.findFirstByEmailOrderByCriadoDesc("ana@exemplo.com")).thenReturn(Optional.empty());
		when(codigos.countByEmailAndCriadoAfter(anyString(), any())).thenReturn(3L);

		var erro = assertThrows(ResponseStatusException.class, () -> service.solicitar("ana@exemplo.com"));

		assertEquals(HttpStatus.TOO_MANY_REQUESTS, erro.getStatusCode());
		verify(envio, never()).enviar(anyString(), anyString(), anyString());
	}

	@Test
	void usuarioValidoRecebeDesafioEOCodigoEEnviado() {
		when(usuarios.buscarAtivoParaAtualizacao("ana@exemplo.com")).thenReturn(Optional.of(new Usuario()));
		when(codigos.findFirstByEmailOrderByCriadoDesc("ana@exemplo.com")).thenReturn(Optional.empty());
		when(codigos.countByEmailAndCriadoAfter(anyString(), any())).thenReturn(0L);

		Map<String, Object> resposta = service.solicitar("ana@exemplo.com");

		assertNotNull(resposta.get("desafio"));
		verify(codigos).save(any(CodigoEmail.class));
		verify(envio).enviar(anyString(), anyString(), anyString());
	}
}