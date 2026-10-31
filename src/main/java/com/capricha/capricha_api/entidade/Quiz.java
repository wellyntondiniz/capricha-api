package com.capricha.capricha_api.entidade;

import java.time.Instant;

import com.capricha.capricha_api.model.Palestra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Quiz de uma palestra. As perguntas são as perguntas ativas cadastradas nessa palestra. */
@Entity
@Table(name = "quiz")
public class Quiz {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "titulo", nullable = false, length = 255)
	private String titulo;

	@Column(name = "descricao", length = 255)
	private String descricao;

	/** Pontos para quem acerta todas as perguntas; quem acerta parte recebe a parte proporcional. */
	@Column(name = "pontuacao", nullable = false)
	private int pontuacao;

	@ManyToOne(optional = false, fetch = FetchType.LAZY)
	@JoinColumn(name = "palestra_id", nullable = false)
	private Palestra palestra;

	@Column(name = "ativo", nullable = false)
	private boolean ativo = true;

	@Column(name = "criado_em", nullable = false)
	private Instant criadoEm = Instant.now();

	protected Quiz() {
	}

	public Quiz(String titulo, String descricao, int pontuacao, Palestra palestra) {
		this.titulo = titulo;
		this.descricao = descricao;
		this.pontuacao = pontuacao;
		this.palestra = palestra;
	}

	/** Edita os dados do quiz. A palestra não muda: o quiz pertence a ela. */
	public void atualizar(String titulo, String descricao, int pontuacao) {
		this.titulo = titulo;
		this.descricao = descricao;
		this.pontuacao = pontuacao;
	}

	public Long getId() { return id; }
	public String getTitulo() { return titulo; }
	public String getDescricao() { return descricao; }
	public int getPontuacao() { return pontuacao; }
	public Palestra getPalestra() { return palestra; }
	public boolean isAtivo() { return ativo; }
	public Instant getCriadoEm() { return criadoEm; }
}