package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capricha.capricha_api.repository.UsuarioRepository;
import com.capricha.capricha_api.entidade.Usuario;

@Service
public class UsuarioService {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	public List<Usuario> getUsuarios() {
		return usuarioRepository.findAllByAtivo(true);
	}
	
}
