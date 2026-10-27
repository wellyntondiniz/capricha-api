package com.capricha.capricha_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Resumo;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.ResumoRepository;
import com.capricha.capricha_api.service.ResumoService.DadosResumo;

class ResumoServiceTest {

	private ResumoRepository resumoRepository;
	private EventoRepository eventoRepository;
	private ArmazenamentoArquivoService armazenamento;
	private ResumoService service;

	@BeforeEach
	void configurar() {
		resumoRepository = mock(ResumoRepository.class);
		eventoRepository = mock(EventoRepository.class);
		armazenamento = mock(ArmazenamentoArquivoService.class);
		service = new ResumoService(resumoRepository, eventoRepository, armazenamento);

		Evento evento = new Evento();
		evento.setId(1);
		evento.setNome("Semana Acadêmica");
		when(eventoRepository.findById(1)).thenReturn(Optional.of(evento));
		when(resumoRepository.save(any(Resumo.class))).thenAnswer(inv -> inv.getArgument(0));
	}

	private DadosResumo dados(String titulo, String autores) {
		return new DadosResumo(titulo, "Texto completo do resumo.", autores, "ia, saúde", 1);
	}

	private int status(ResponseStatusException e) {
		return e.getStatusCode().value();
	}

	@Test
	void separaPorVirgulaPontoEVirgulaEQuebraDeLinhaSemRepetidos() {
		assertEquals(List.of("Ana", "Bruno", "Carla"), ResumoService.separar(" Ana, bruno ;\nCarla,ANA,, "));
	}

	@Test
	void cadastraResumoValidoVinculadoAoEvento() {
		var resposta = service.cadastrar(dados("Título A", "Ana; Bruno"), null);

		assertEquals("Título A", resposta.titulo());
		assertEquals(List.of("Ana", "Bruno"), resposta.autores());
		assertEquals(List.of("ia", "saúde"), resposta.palavrasChave());
		assertEquals(1, resposta.eventoId());
	}

	@Test
	void semAutorDevolve400() {
		var erro = assertThrows(ResponseStatusException.class, () -> service.cadastrar(dados("Título", " , "), null));
		assertEquals(400, status(erro));
	}

	@Test
	void semTituloDevolve400() {
		var erro = assertThrows(ResponseStatusException.class, () -> service.cadastrar(dados("  ", "Ana"), null));
		assertEquals(400, status(erro));
	}

	@Test
	void eventoInexistenteDevolve404() {
		var erro = assertThrows(ResponseStatusException.class,
				() -> service.cadastrar(new DadosResumo("T", "x", "Ana", "", 99), null));
		assertEquals(404, status(erro));
	}

	@Test
	void eventoDesativadoDevolve400() {
		Evento inativo = new Evento();
		inativo.setId(2);
		inativo.setAtivo(false);
		when(eventoRepository.findById(2)).thenReturn(Optional.of(inativo));

		var erro = assertThrows(ResponseStatusException.class,
				() -> service.cadastrar(new DadosResumo("T", "x", "Ana", "", 2), null));
		assertEquals(400, status(erro));
	}

	@Test
	void tituloRepetidoNoMesmoEventoDevolve409() {
		when(resumoRepository.existsByEventoIdAndTituloIgnoreCase(1, "Título")).thenReturn(true);

		var erro = assertThrows(ResponseStatusException.class, () -> service.cadastrar(dados("Título", "Ana"), null));
		assertEquals(409, status(erro));
	}

	@Test
	void bannerQueNaoEImagemEhRecusadoSemGravarArquivo() {
		var arquivo = new MockMultipartFile("banner", "pagina.html", "text/html", "<script></script>".getBytes());

		var erro = assertThrows(ResponseStatusException.class,
				() -> service.cadastrar(dados("Título", "Ana"), arquivo));

		assertEquals(400, status(erro));
		verify(armazenamento, never()).salvar(any());
	}

	@Test
	void bannerImagemEhGravado() {
		var arquivo = new MockMultipartFile("banner", "capa.png", "image/png", new byte[] { 1, 2, 3 });
		when(armazenamento.salvar(arquivo)).thenReturn("abc.png");

		var resposta = service.cadastrar(dados("Título", "Ana"), arquivo);

		assertEquals("abc.png", resposta.banner());
	}
}
