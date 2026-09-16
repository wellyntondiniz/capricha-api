package com.capricha.capricha_api.repository;

import com.capricha.capricha_api.entidade.CodigoEmail;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface CodigoEmailRepository extends JpaRepository<CodigoEmail, String> {
    long countByEmailAndCriadoAfter(String email, Instant inicio);
    Optional<CodigoEmail> findFirstByEmailOrderByCriadoDesc(String email);
    @Modifying @Query("update CodigoEmail c set c.utilizado = true where c.email = :email")
    void invalidar(@Param("email") String email);
}
