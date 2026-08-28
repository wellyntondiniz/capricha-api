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

import com.capricha.capricha_api.entidade.Usuario;

@RestController
@RequestMapping(value="/usuario")
@CrossOrigin
public class UsuarioRestController {
	
	@Autowired
	UsuarioService usuarioService;
	
	@GetMapping
	public List<Usuario> getUsuarios() {
		return usuarioService.getUsuarios();
	}
	
	@PostMapping
	public Usuario salvar(@Valid @RequestBody Usuario usuario) {
		return usuarioService.cadastrarUsuario(usuario);
	}
}
