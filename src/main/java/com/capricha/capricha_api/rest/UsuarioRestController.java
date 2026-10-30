package com.capricha.capricha_api.rest;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.service.ConfirmacaoCadastroService;
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

	@Autowired
	ConfirmacaoCadastroService confirmacaoCadastroService;
	
	@GetMapping
	public List<UsuarioResponse> getUsuarios() {
		return usuarioService.getUsuarios().stream().map(UsuarioResponse::de).toList();
	}
	
	@PostMapping
	public CadastroUsuarioResponse salvar(@Valid @RequestBody CadastroUsuarioRequest cadastro) {
		// O e-mail só é enviado depois que o cadastro foi gravado com sucesso (erros de validação/duplicidade lançam antes).
		Usuario usuario = usuarioService.cadastrarUsuario(cadastro.paraUsuario());
		return CadastroUsuarioResponse.de(usuario, confirmacaoCadastroService.enviar(usuario));
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