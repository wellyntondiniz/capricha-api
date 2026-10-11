package com.capricha.capricha_api.service;

import java.util.List;
import java.util.Optional;

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
		// A senha nunca é gravada em texto puro: salva apenas o hash (PBKDF2 com salt).
		usuario.setSenha(Senhas.proteger(usuario.getSenha()));
		return salvar(usuario);
	}
	
	// Confere e-mail e senha comparando com o hash armazenado.
	// A mensagem é a mesma para e-mail inexistente e senha errada, para não revelar quais e-mails existem.
	public Usuario autenticar(String email, String senha) {
		Optional<Usuario> usuario = usuarioRepository.findFirstByEmailIgnoreCaseAndAtivoTrue(email.trim());
		String hashArmazenado = usuario.map(Usuario::getSenha).orElse(HASH_FICTICIO);
		// Mesmo sem usuário, calcula o hash para que o tempo de resposta não denuncie o e-mail.
		boolean senhaConfere = Senhas.confere(senha, hashArmazenado);
		if (usuario.isEmpty() || !senhaConfere) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
		}
		return usuario.get();
	}
	
	private static final String HASH_FICTICIO = Senhas.proteger("senha-ficticia-para-tempo-constante");
	
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
