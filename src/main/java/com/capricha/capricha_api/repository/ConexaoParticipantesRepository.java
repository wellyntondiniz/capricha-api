package com.capricha.capricha_api.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.capricha.capricha_api.entidade.ConexaoParticipantes;
import com.capricha.capricha_api.entidade.ConexaoParticipantes.Status;
import jakarta.persistence.LockModeType;

public interface ConexaoParticipantesRepository extends JpaRepository<ConexaoParticipantes, Long> {
    boolean existsByChavePendente(String chave);
    boolean existsByChaveConfirmada(String chave);
    Optional<ConexaoParticipantes> findByChavePendente(String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConexaoParticipantes c where c.id = :id")
    Optional<ConexaoParticipantes> buscarParaResposta(@Param("id") Long id);

    @Query("select c from ConexaoParticipantes c join fetch c.evento join fetch c.solicitante join fetch c.destinatario "
            + "where c.destinatario.id = :usuarioId and c.status = :status order by c.criadaEm desc")
    List<ConexaoParticipantes> doDestinatario(@Param("usuarioId") Integer usuarioId, @Param("status") Status status);

    @Query("select c from ConexaoParticipantes c join fetch c.evento join fetch c.solicitante join fetch c.destinatario "
            + "where c.solicitante.id = :usuarioId and c.status = :status order by c.criadaEm desc")
    List<ConexaoParticipantes> doSolicitante(@Param("usuarioId") Integer usuarioId, @Param("status") Status status);

    @Query("select c from ConexaoParticipantes c join fetch c.evento join fetch c.solicitante join fetch c.destinatario "
            + "where c.status = :status and (c.solicitante.id = :usuarioId or c.destinatario.id = :usuarioId) "
            + "order by c.respondidaEm desc")
    List<ConexaoParticipantes> doUsuario(@Param("usuarioId") Integer usuarioId, @Param("status") Status status);

    /**
     * Soma XP direto no banco. A soma é feita pelo próprio UPDATE (sobre o valor mais recente),
     * então duas confirmações simultâneas não sobrescrevem a pontuação uma da outra.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Participacao p set p.score = coalesce(p.score, 0) + :xp where p.id = :id")
    int somarScore(@Param("id") Integer participacaoId, @Param("xp") int xp);
}