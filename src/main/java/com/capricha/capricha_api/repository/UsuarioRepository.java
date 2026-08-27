package com.capricha.capricha_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioRepository, Integer> {
	
	public List<Usuario> findAllByAtivo(Boolean ativo);
	
}