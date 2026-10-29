package com.capricha.capricha_api.entidade;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "avaliacao_aplicativo")
public class AvaliacaoAplicativo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer navegacao;

    @Column(nullable = false)
    private Integer clarezaInformacoes;

    @Column(nullable = false)
    private Integer utilidadeFuncionalidades;

    @Column(nullable = false)
    private Integer desempenhoTecnico;

    @Column(nullable = false)
    private Integer recomendacao;

    @Column(length = 1000)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private Instant respondidaEm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Integer getNavegacao() { return navegacao; }
    public void setNavegacao(Integer navegacao) { this.navegacao = navegacao; }
    public Integer getClarezaInformacoes() { return clarezaInformacoes; }
    public void setClarezaInformacoes(Integer clarezaInformacoes) { this.clarezaInformacoes = clarezaInformacoes; }
    public Integer getUtilidadeFuncionalidades() { return utilidadeFuncionalidades; }
    public void setUtilidadeFuncionalidades(Integer utilidadeFuncionalidades) { this.utilidadeFuncionalidades = utilidadeFuncionalidades; }
    public Integer getDesempenhoTecnico() { return desempenhoTecnico; }
    public void setDesempenhoTecnico(Integer desempenhoTecnico) { this.desempenhoTecnico = desempenhoTecnico; }
    public Integer getRecomendacao() { return recomendacao; }
    public void setRecomendacao(Integer recomendacao) { this.recomendacao = recomendacao; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public Instant getRespondidaEm() { return respondidaEm; }
    public void setRespondidaEm(Instant respondidaEm) { this.respondidaEm = respondidaEm; }
}
