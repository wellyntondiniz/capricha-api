package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.repository.UsuarioRepository;
import com.capricha.capricha_api.entidade.Usuario;

@Service
public class UsuarioService {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	public List<Usuario> getUsuarios() {
		return usuarioRepository.findAllByAtivo(true);
	}
	
	public Usuario getUsuarioById(Integer id) {
		Usuario usuario = usuarioRepository.findById(id).get();
		return usuario;
	}
	
	public Usuario cadastrarUsuario(Usuario usuario) {
		validarCadastro(usuario);
		return salvar(usuario);
	}
	
	public Usuario salvar(Usuario usuario) {
		return usuarioRepository.save(usuario);
	}
	
	private void validarCadastro(Usuario usuario) {
	    if (usuarioRepository.existsByEmail(usuario.getEmail())) {
	    	throw new ResponseStatusException(
	    	        HttpStatus.CONFLICT,
	    	        "Email já cadastrado!"
	    	    );
	    }
	}
}
