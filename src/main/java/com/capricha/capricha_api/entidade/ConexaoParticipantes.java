package com.capricha.capricha_api.entidade;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "conexao_participantes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conexao_pendente", columnNames = "chave_pendente"),
        @UniqueConstraint(name = "uk_conexao_confirmada", columnNames = "chave_confirmada")})
public class ConexaoParticipantes {
    public enum Status { PENDENTE, ACEITA, RECUSADA }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destinatario_id", nullable = false)
    private Usuario destinatario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 12)
    private Status status;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "respondida_em")
    private Instant respondidaEm;

    @Column(name = "xp_concedido")
    private Integer xpConcedido;

    @Column(name = "chave_pendente", length = 64)
    private String chavePendente;

    @Column(name = "chave_confirmada", length = 64)
    private String chaveConfirmada;

    protected ConexaoParticipantes() {}

    public ConexaoParticipantes(Evento evento, Usuario solicitante, Usuario destinatario, Instant agora, String chave) {
        this.evento = evento;
        this.solicitante = solicitante;
        this.destinatario = destinatario;
        this.criadaEm = agora;
        this.status = Status.PENDENTE;
        this.chavePendente = chave;
    }

    public void aceitar(Instant agora, int xp) {
        this.status = Status.ACEITA;
        this.chaveConfirmada = this.chavePendente;
        this.chavePendente = null;
        this.respondidaEm = agora;
        this.xpConcedido = xp;
    }

    public void recusar(Instant agora) {
        this.status = Status.RECUSADA;
        this.chavePendente = null;
        this.respondidaEm = agora;
    }

    public Long getId() { return id; }
    public Evento getEvento() { return evento; }
    public Usuario getSolicitante() { return solicitante; }
    public Usuario getDestinatario() { return destinatario; }
    public Status getStatus() { return status; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getRespondidaEm() { return respondidaEm; }
    public Integer getXpConcedido() { return xpConcedido; }
    public String getChavePendente() { return chavePendente; }
}