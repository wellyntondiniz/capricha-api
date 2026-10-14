package com.capricha.capricha_api.entidade;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "resumo")
public class Resumo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "titulo", nullable = false, length = 200)
	private String titulo;

	@Column(name = "texto", nullable = false, columnDefinition = "TEXT")
	private String texto;

	@ElementCollection
	@CollectionTable(name = "resumo_autor", joinColumns = @JoinColumn(name = "resumo_id"))
	@Column(name = "nome", length = 120)
	@OrderColumn(name = "posicao")
	private List<String> autores = new ArrayList<>();

	@Column(name = "banner")
	private String banner;

	@ElementCollection
	@CollectionTable(name = "resumo_palavra_chave", joinColumns = @JoinColumn(name = "resumo_id"))
	@Column(name = "palavra", length = 60)
	@OrderColumn(name = "posicao")
	private List<String> palavrasChave = new ArrayList<>();

	@ManyToOne(optional = false)
	@JoinColumn(name = "evento_id", nullable = false)
	private Evento evento;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitulo() {
		return titulo;
	}

	public void setTitulo(String titulo) {
		this.titulo = titulo;
	}

	public String getTexto() {
		return texto;
	}

	public void setTexto(String texto) {
		this.texto = texto;
	}

	public List<String> getAutores() {
		return autores;
	}

	public void setAutores(List<String> autores) {
		this.autores = autores;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}

	public List<String> getPalavrasChave() {
		return palavrasChave;
	}

	public void setPalavrasChave(List<String> palavrasChave) {
		this.palavrasChave = palavrasChave;
	}

	public Evento getEvento() {
		return evento;
	}

	public void setEvento(Evento evento) {
		this.evento = evento;
	}
}
