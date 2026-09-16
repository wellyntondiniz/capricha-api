package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.entidade.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

/** Repositório exclusivo da história CA-02; não altera o repositório do cadastro. */
public interface RecuperacaoUsuarioRepository extends JpaRepository<Usuario, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where lower(u.email) = :email and u.ativo = true")
    Optional<Usuario> buscarAtivoParaAtualizacao(@Param("email") String email);
}
