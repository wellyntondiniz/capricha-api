package com.capricha.capricha_api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.capricha.capricha_api.entidade.CodigoConexao;

public interface CodigoConexaoRepository extends JpaRepository<CodigoConexao, Long> {
    Optional<CodigoConexao> findByUsuarioId(Integer usuarioId);
    Optional<CodigoConexao> findByToken(String token);
}