package com.capricha.capricha_api.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.capricha.capricha_api.service.ResumoService;
import com.capricha.capricha_api.service.ResumoService.DadosResumo;

@RestController
@RequestMapping("/resumos")
@CrossOrigin
public class ResumoRestController {

	private final ResumoService resumoService;

	public ResumoRestController(ResumoService resumoService) {
		this.resumoService = resumoService;
	}

	@GetMapping
	public List<ResumoResponse> listar(@RequestParam("eventoId") Integer eventoId) {
		return resumoService.listarPorEvento(eventoId);
	}

	@GetMapping("/{id}")
	public ResumoResponse buscar(@PathVariable Integer id) {
		return resumoService.buscar(id);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ResumoResponse cadastrar(
			@RequestParam(value = "titulo", required = false) String titulo,
			@RequestParam(value = "texto", required = false) String texto,
			@RequestParam(value = "autores", required = false) String autores,
			@RequestParam(value = "palavrasChave", required = false) String palavrasChave,
			@RequestParam(value = "eventoId", required = false) Integer eventoId,
			@RequestParam(value = "banner", required = false) MultipartFile banner) {
		return resumoService.cadastrar(new DadosResumo(titulo, texto, autores, palavrasChave, eventoId), banner);
	}

	@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResumoResponse atualizar(
			@PathVariable Integer id,
			@RequestParam(value = "titulo", required = false) String titulo,
			@RequestParam(value = "texto", required = false) String texto,
			@RequestParam(value = "autores", required = false) String autores,
			@RequestParam(value = "palavrasChave", required = false) String palavrasChave,
			@RequestParam(value = "eventoId", required = false) Integer eventoId,
			@RequestParam(value = "removerBanner", defaultValue = "false") boolean removerBanner,
			@RequestParam(value = "banner", required = false) MultipartFile banner) {
		return resumoService.atualizar(id, new DadosResumo(titulo, texto, autores, palavrasChave, eventoId), banner,
				removerBanner);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void excluir(
			@PathVariable Integer id) {
		resumoService.excluir(id);
	}
}
