package com.capricha.capricha_api.rest;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.capricha.capricha_api.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value="/usuario")
@CrossOrigin
public class UsuarioRestController {
	
	@Autowired
	UsuarioService usuarioService;
	
	@GetMapping
	public List<UsuarioResponse> getUsuarios() {
		return usuarioService.getUsuarios().stream().map(UsuarioResponse::de).toList();
	}
	
	@PostMapping
	public UsuarioResponse salvar(@Valid @RequestBody CadastroUsuarioRequest cadastro) {
		return UsuarioResponse.de(usuarioService.cadastrarUsuario(cadastro.paraUsuario()));
	}

	@PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UsuarioResponse atualizarFoto(
			@PathVariable Integer id,
			@RequestParam("foto") MultipartFile foto
	) {
		return UsuarioResponse.de(usuarioService.atualizarFoto(id, foto));
	}

	@DeleteMapping("/{id}/foto")
	public UsuarioResponse removerFoto(@PathVariable Integer id) {
		return UsuarioResponse.de(usuarioService.removerFoto(id));
	}
}
