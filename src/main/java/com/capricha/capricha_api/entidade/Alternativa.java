package com.capricha.capricha_api.entidade;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "alternativa")
public class Alternativa {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "texto")
	private String texto;
	
	@Column(name = "correta")
	private boolean correta;
	
	@ManyToOne
	@JoinColumn(name = "pergunta_id", nullable = false)
	private Pergunta pergunta;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getTexto() {
		return texto;
	}
	
	public void setTexto(String texto) {
		this.texto = texto;
	}
	
	public boolean getCorreta() {
		return correta;
	}
	
	public void setCorreta(boolean correta) {
		this.correta = correta;
	}
	
	@JsonIgnore
	public Pergunta getPergunta() {
		return pergunta;
	}
	
	public void setPergunta(Pergunta pergunta) {
		this.pergunta = pergunta;
	}
}
