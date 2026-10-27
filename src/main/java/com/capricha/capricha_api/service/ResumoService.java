package com.capricha.capricha_api.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Resumo;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.ResumoRepository;
import com.capricha.capricha_api.rest.ResumoResponse;

@Service
public class ResumoService {

	static final int MAX_TITULO = 200;
	static final int MAX_TEXTO = 20000;
	static final int MAX_AUTORES = 20;
	static final int MAX_AUTOR = 120;
	static final int MAX_PALAVRAS = 15;
	static final int MAX_PALAVRA = 60;

	private static final Set<String> TIPOS_IMAGEM = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
	private static final Set<String> EXTENSOES_IMAGEM = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");

	public record DadosResumo(String titulo, String texto, String autores, String palavrasChave, Integer eventoId) {
	}

	private final ResumoRepository resumoRepository;
	private final EventoRepository eventoRepository;
	private final ArmazenamentoArquivoService armazenamento;

	public ResumoService(ResumoRepository resumoRepository, EventoRepository eventoRepository,
			ArmazenamentoArquivoService armazenamento) {
		this.resumoRepository = resumoRepository;
		this.eventoRepository = eventoRepository;
		this.armazenamento = armazenamento;
	}

	@Transactional(readOnly = true)
	public List<ResumoResponse> listarPorEvento(Integer eventoId) {
		if (eventoId == null || !eventoRepository.existsById(eventoId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado!");
		}
		return resumoRepository.findByEventoIdOrderByTituloAsc(eventoId).stream().map(ResumoResponse::de).toList();
	}

	@Transactional(readOnly = true)
	public ResumoResponse buscar(Integer id) {
		return ResumoResponse.de(buscarEntidade(id));
	}

	@Transactional
	public ResumoResponse cadastrar(DadosResumo dados, MultipartFile banner) {
		Evento evento = validarEvento(dados.eventoId());
		Resumo resumo = new Resumo();
		preencher(resumo, dados, evento);

		if (resumoRepository.existsByEventoIdAndTituloIgnoreCase(evento.getId(), resumo.getTitulo())) {
			throw tituloDuplicado();
		}

		String novoBanner = salvarBanner(banner);
		resumo.setBanner(novoBanner);
		try {
			return ResumoResponse.de(resumoRepository.save(resumo));
		} catch (RuntimeException e) {
			removerArquivoSilencioso(novoBanner);
			throw e;
		}
	}

	@Transactional
	public ResumoResponse atualizar(Integer id, DadosResumo dados, MultipartFile banner, boolean removerBanner) {
		Resumo resumo = buscarEntidade(id);
		Evento evento = validarEvento(dados.eventoId());
		preencher(resumo, dados, evento);

		if (resumoRepository.existsByEventoIdAndTituloIgnoreCaseAndIdNot(evento.getId(), resumo.getTitulo(), id)) {
			throw tituloDuplicado();
		}

		String bannerAntigo = resumo.getBanner();
		String novoBanner = salvarBanner(banner);
		if (novoBanner != null) {
			resumo.setBanner(novoBanner);
		} else if (removerBanner) {
			resumo.setBanner(null);
		}

		try {
			Resumo salvo = resumoRepository.save(resumo);
			if (bannerAntigo != null && !bannerAntigo.equals(salvo.getBanner())) {
				removerArquivoAposCommit(bannerAntigo);
			}
			return ResumoResponse.de(salvo);
		} catch (RuntimeException e) {
			removerArquivoSilencioso(novoBanner);
			throw e;
		}
	}

	@Transactional
	public void excluir(Integer id) {
		Resumo resumo = buscarEntidade(id);
		String banner = resumo.getBanner();
		resumoRepository.delete(resumo);
		if (banner != null) {
			removerArquivoAposCommit(banner);
		}
	}

	private Resumo buscarEntidade(Integer id) {
		return resumoRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resumo não encontrado!"));
	}

	private Evento validarEvento(Integer eventoId) {
		if (eventoId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "É necessário informar o evento!");
		}
		Evento evento = eventoRepository.findById(eventoId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evento não encontrado!"));
		if (!evento.getAtivo()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Este evento está desativado e não aceita novos resumos.");
		}
		return evento;
	}

	private void preencher(Resumo resumo, DadosResumo dados, Evento evento) {
		String titulo = dados.titulo() == null ? "" : dados.titulo().trim();
		String texto = dados.texto() == null ? "" : dados.texto().trim();

		if (titulo.isEmpty()) {
			throw requisicaoInvalida("Título é obrigatório.");
		}
		if (titulo.length() > MAX_TITULO) {
			throw requisicaoInvalida("O título deve ter no máximo " + MAX_TITULO + " caracteres.");
		}
		if (texto.isEmpty()) {
			throw requisicaoInvalida("O texto do resumo é obrigatório.");
		}
		if (texto.length() > MAX_TEXTO) {
			throw requisicaoInvalida("O texto deve ter no máximo " + MAX_TEXTO + " caracteres.");
		}

		List<String> autores = separar(dados.autores());
		if (autores.isEmpty()) {
			throw requisicaoInvalida("Informe ao menos um autor.");
		}
		validarLista(autores, MAX_AUTORES, MAX_AUTOR, "autores", "autor");

		List<String> palavrasChave = separar(dados.palavrasChave());
		validarLista(palavrasChave, MAX_PALAVRAS, MAX_PALAVRA, "palavras-chave", "palavra-chave");

		resumo.setTitulo(titulo);
		resumo.setTexto(texto);
		resumo.setAutores(autores);
		resumo.setPalavrasChave(palavrasChave);
		resumo.setEvento(evento);
	}

	private void validarLista(List<String> itens, int maxItens, int maxTamanho, String plural, String singular) {
		if (itens.size() > maxItens) {
			throw requisicaoInvalida("Informe no máximo " + maxItens + " " + plural + ".");
		}
		for (String item : itens) {
			if (item.length() > maxTamanho) {
				throw requisicaoInvalida("Cada " + singular + " deve ter no máximo " + maxTamanho + " caracteres.");
			}
		}
	}

	static List<String> separar(String bruto) {
		List<String> resultado = new ArrayList<>();
		if (bruto == null || bruto.isBlank()) {
			return resultado;
		}
		Set<String> vistos = new HashSet<>();
		for (String parte : bruto.split("[\\r\\n;,]+")) {
			String item = parte.trim();
			if (!item.isEmpty() && vistos.add(item.toLowerCase(Locale.ROOT))) {
				resultado.add(item);
			}
		}
		return resultado;
	}

	private String salvarBanner(MultipartFile banner) {
		if (banner == null || banner.isEmpty()) {
			return null;
		}
		String tipo = banner.getContentType() == null ? "" : banner.getContentType().toLowerCase(Locale.ROOT);
		String nome = banner.getOriginalFilename() == null ? "" : banner.getOriginalFilename().toLowerCase(Locale.ROOT);
		int ponto = nome.lastIndexOf('.');
		String extensao = ponto >= 0 ? nome.substring(ponto) : "";

		if (!TIPOS_IMAGEM.contains(tipo) || !EXTENSOES_IMAGEM.contains(extensao)) {
			throw requisicaoInvalida("O banner deve ser uma imagem JPG, PNG, WEBP ou GIF.");
		}
		return armazenamento.salvar(banner);
	}

	private void removerArquivoAposCommit(String nomeArquivo) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					removerArquivoSilencioso(nomeArquivo);
				}
			});
		} else {
			removerArquivoSilencioso(nomeArquivo);
		}
	}

	private void removerArquivoSilencioso(String nomeArquivo) {
		if (nomeArquivo == null) {
			return;
		}
		try {
			armazenamento.remover(nomeArquivo);
		} catch (RuntimeException ignorada) {
		}
	}

	private ResponseStatusException requisicaoInvalida(String mensagem) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
	}

	private ResponseStatusException tituloDuplicado() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um resumo com este título neste evento.");
	}
}
