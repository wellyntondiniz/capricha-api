package com.capricha.capricha_api.entidade;

import jakarta.persistence.*;

@Entity
@Table(name = "codigo_conexao", uniqueConstraints = {
        @UniqueConstraint(name = "uk_codigo_conexao_usuario", columnNames = "usuario_id"),
        @UniqueConstraint(name = "uk_codigo_conexao_token", columnNames = "token")})
public class CodigoConexao {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "token", nullable = false, length = 36)
    private String token;

    protected CodigoConexao() {}

    public CodigoConexao(Usuario usuario, String token) {
        this.usuario = usuario;
        this.token = token;
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public String getToken() { return token; }
}