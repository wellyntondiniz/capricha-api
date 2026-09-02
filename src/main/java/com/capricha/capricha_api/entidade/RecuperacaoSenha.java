package com.capricha.capricha_api.entidade;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recuperacao_senha")
public class RecuperacaoSenha {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String email;
    @Column(nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false) private LocalDateTime expiraEm;
    @Column(nullable = false) private boolean utilizado;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public void setExpiraEm(LocalDateTime expiraEm) { this.expiraEm = expiraEm; }
    public boolean isUtilizado() { return utilizado; }
    public void setUtilizado(boolean utilizado) { this.utilizado = utilizado; }
}
