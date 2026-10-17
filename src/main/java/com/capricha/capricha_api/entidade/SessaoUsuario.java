package com.capricha.capricha_api.entidade;

import java.time.Instant;
import jakarta.persistence.*;

/** Credencial opaca: somente o hash é persistido; não duplica o cadastro de usuário. */
@Entity
@Table(name = "sessao_usuario")
public class SessaoUsuario {
    @Id
    @Column(length = 64)
    private String tokenHash;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Instant expiraEm;

    @Column(nullable = false, length = 64)
    private String senhaFingerprint;

    protected SessaoUsuario() {}

    public SessaoUsuario(String tokenHash, Usuario usuario, Instant expiraEm, String senhaFingerprint) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEm = expiraEm;
        this.senhaFingerprint = senhaFingerprint;
    }

    public Usuario getUsuario() { return usuario; }
    public Instant getExpiraEm() { return expiraEm; }
    public String getSenhaFingerprint() { return senhaFingerprint; }
}
