package com.capricha.capricha_api.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.service.ArmazenamentoArquivoService;
import com.capricha.capricha_api.service.EventoService;

import jakarta.validation.Valid;

/**
 * Páginas HTML (Thymeleaf) de cadastro de evento. Não mexe no
 * EventoRestController (que continua servindo o app em JSON) — só reaproveita
 * o EventoService que já existia.
 */
@Controller
@RequestMapping("/eventos")
public class EventoWebController {

	@Autowired
	private EventoService eventoService;

	@Autowired
	private ArmazenamentoArquivoService armazenamentoArquivoService;

	@GetMapping
	public String listar(Model model) {
		model.addAttribute("eventos", eventoService.getEventos());
		return "eventos/lista";
	}

	@GetMapping("/{id}")
	public String visualizar(@PathVariable Integer id, Model model) {
		model.addAttribute("evento", eventoService.getEventoById(id));
		return "eventos/visualizar";
	}

	@GetMapping("/novo")
	public String novo(Model model) {
		model.addAttribute("evento", new Evento());
		model.addAttribute("hoje", LocalDate.now());
		return "eventos/formulario";
	}

	@GetMapping("/{id}/editar")
	public String editar(@PathVariable Integer id, Model model) {
		model.addAttribute("evento", eventoService.getEventoById(id));
		return "eventos/formulario";
	}

	@PostMapping
	public String cadastrar(
			@Valid @ModelAttribute("evento") Evento evento,
			BindingResult bindingResult,
			@RequestParam(value = "arquivoFotoPerfil", required = false) MultipartFile arquivoFotoPerfil,
			@RequestParam(value = "arquivoBanner", required = false) MultipartFile arquivoBanner,
			@RequestParam(value = "participantesTexto", required = false) String participantesTexto,
			@RequestParam(value = "atividadesTexto", required = false) String atividadesTexto,
			Model model) {
		model.addAttribute("hoje", LocalDate.now());
		if (bindingResult.hasErrors()) {
			return "eventos/formulario";
		}
		try {
			evento.setFotoPerfil(armazenamentoArquivoService.salvar(arquivoFotoPerfil));
			evento.setBanner(armazenamentoArquivoService.salvar(arquivoBanner));
			evento.setParticipantes(dividirLinhas(participantesTexto));
			evento.setAtividades(dividirLinhas(atividadesTexto));
			eventoService.cadastrarEvento(evento);
		} catch (ResponseStatusException e) {
			model.addAttribute("erro", e.getReason());
			return "eventos/formulario";
		}
		return "redirect:/eventos";
	}

	@PostMapping("/{id}")
	public String atualizar(
			@PathVariable Integer id,
			@Valid @ModelAttribute("evento") Evento evento,
			BindingResult bindingResult,
			@RequestParam(value = "arquivoFotoPerfil", required = false) MultipartFile arquivoFotoPerfil,
			@RequestParam(value = "arquivoBanner", required = false) MultipartFile arquivoBanner,
			@RequestParam(value = "participantesTexto", required = false) String participantesTexto,
			@RequestParam(value = "atividadesTexto", required = false) String atividadesTexto,
			Model model) {
		if (bindingResult.hasErrors()) {
			return "eventos/formulario";
		}
		try {
			// Só troca a imagem se o usuário escolheu um arquivo novo;
			// senão o EventoService mantém a que já estava salva.
			evento.setFotoPerfil(armazenamentoArquivoService.salvar(arquivoFotoPerfil));
			evento.setBanner(armazenamentoArquivoService.salvar(arquivoBanner));
			evento.setParticipantes(dividirLinhas(participantesTexto));
			evento.setAtividades(dividirLinhas(atividadesTexto));
			eventoService.atualizarEvento(id, evento);
		} catch (ResponseStatusException e) {
			model.addAttribute("erro", e.getReason());
			return "eventos/formulario";
		}
		return "redirect:/eventos";
	}

	@PostMapping("/{id}/desativar")
	public String desativar(@PathVariable Integer id) {
		eventoService.desativarEvento(id);
		return "redirect:/eventos";
	}

	/**
	 * Transforma o texto do textarea (um item por linha) numa lista,
	 * ignorando linhas em branco.
	 */
	private List<String> dividirLinhas(String texto) {
		if (texto == null || texto.isBlank()) {
			return new ArrayList<>();
		}
		List<String> linhas = new ArrayList<>();
		for (String linha : texto.split("\\r?\\n")) {
			String linhaTratada = linha.trim();
			if (!linhaTratada.isEmpty()) {
				linhas.add(linhaTratada);
			}
		}
		return linhas;
	}
}
