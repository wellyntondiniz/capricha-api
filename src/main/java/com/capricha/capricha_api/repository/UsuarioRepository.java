package com.capricha.capricha_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.capricha.capricha_api.entidade.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
	
	public List<Usuario> findAllByAtivo(Boolean ativo);
	
	boolean existsByEmail(String email);
	
	Optional<Usuario> findFirstByEmailIgnoreCaseAndAtivoTrue(String email);
	
	List<Usuario> findBySenhaNotLike(String prefixo);
	
}