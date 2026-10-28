package com.capricha.capricha_api.entidade;

import java.time.Instant;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


@Entity
@Table(name = "voto_resumo", uniqueConstraints = @UniqueConstraint(name = "uk_voto_usuario_resumo", columnNames = {
		"usuario_id", "resumo_id" }))
public class VotoResumo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	// Excluir um resumo remove os votos dados a ele (quem votou nele volta a ter o voto do evento).
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "resumo_id", nullable = false)
	@OnDelete(action = OnDeleteAction.CASCADE)
	private Resumo resumo;

	// Cópia do evento do resumo: permite contar os votos do usuário por evento.
	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	@Column(name = "votado_em", nullable = false)
	private Instant votadoEm;

	protected VotoResumo() {
	}

	public VotoResumo(Usuario usuario, Resumo resumo, Evento evento, Instant votadoEm) {
		this.usuario = usuario;
		this.resumo = resumo;
		this.evento = evento;
		this.votadoEm = votadoEm;
	}

	public Long getId() {
		return id;
	}

	public Usuario getUsuario() {
		return usuario;
	}

	public Resumo getResumo() {
		return resumo;
	}

	public Evento getEvento() {
		return evento;
	}

	public Instant getVotadoEm() {
		return votadoEm;
	}
}