package com.capricha.capricha_api.entidade;

import java.time.Instant;

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

/** Resultado do usuário em um quiz. A restrição única garante uma única tentativa por usuário e quiz. */
@Entity
@Table(name = "resposta_quiz", uniqueConstraints = @UniqueConstraint(
		name = "uk_resposta_quiz_usuario", columnNames = { "usuario_id", "quiz_id" }))
public class RespostaQuiz {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id", nullable = false)
	private Usuario usuario;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "quiz_id", nullable = false)
	private Quiz quiz;

	@Column(name = "acertos", nullable = false)
	private int acertos;

	@Column(name = "total_perguntas", nullable = false)
	private int totalPerguntas;

	@Column(name = "pontos_obtidos", nullable = false)
	private int pontosObtidos;

	@Column(name = "respondido_em", nullable = false)
	private Instant respondidoEm;

	protected RespostaQuiz() {
	}

	public RespostaQuiz(Usuario usuario, Quiz quiz, int acertos, int totalPerguntas, int pontosObtidos, Instant respondidoEm) {
		this.usuario = usuario;
		this.quiz = quiz;
		this.acertos = acertos;
		this.totalPerguntas = totalPerguntas;
		this.pontosObtidos = pontosObtidos;
		this.respondidoEm = respondidoEm;
	}

	public Long getId() { return id; }
	public Usuario getUsuario() { return usuario; }
	public Quiz getQuiz() { return quiz; }
	public int getAcertos() { return acertos; }
	public int getTotalPerguntas() { return totalPerguntas; }
	public int getPontosObtidos() { return pontosObtidos; }
	public Instant getRespondidoEm() { return respondidoEm; }
}
