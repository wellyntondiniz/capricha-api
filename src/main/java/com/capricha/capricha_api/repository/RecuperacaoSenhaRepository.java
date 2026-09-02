package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.entidade.RecuperacaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    Optional<RecuperacaoSenha> findByTokenHashAndUtilizadoFalse(String tokenHash);
}
