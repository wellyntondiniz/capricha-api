package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.entidade.SessaoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoUsuarioRepository extends JpaRepository<SessaoUsuario, String> {}
