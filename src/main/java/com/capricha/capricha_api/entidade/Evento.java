package com.capricha.capricha_api.entidade;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "evento")
public class Evento {

	private static final DateTimeFormatter FORMATO_EXIBICAO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "nome")
	@NotBlank(message = "Nome é obrigatório!")
	private String nome;

	@Column(name = "descricao")
	private String descricao;

	@Column(name = "data_inicio")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime dataInicio;

	@Column(name = "data_termino")
	@DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
	private LocalDateTime dataTermino;

	@Column(name = "ativo")
	private boolean ativo = true;

	/**
	 * Guardamos só o nome do arquivo (ex: "a1b2c3.png"); o arquivo em si fica
	 * salvo em disco, fora do banco. Ver ArmazenamentoArquivoService.
	 */
	@Column(name = "foto_perfil")
	private String fotoPerfil;

	@Column(name = "banner")
	private String banner;

	/**
	 * Lista simples de nomes (participantes/palestrantes), sem CRUD próprio —
	 * é só informativo dentro do evento. Não confundir com o cadastro de
	 * palestra "de verdade" (CA-07), que é outra funcionalidade.
	 */
	@ElementCollection
	@CollectionTable(name = "evento_participante", joinColumns = @JoinColumn(name = "evento_id"))
	@Column(name = "nome")
	@OrderColumn(name = "posicao")
	private List<String> participantes = new ArrayList<>();

	/**
	 * Lista simples de tópicos das atividades do dia do evento.
	 */
	@ElementCollection
	@CollectionTable(name = "evento_atividade", joinColumns = @JoinColumn(name = "evento_id"))
	@Column(name = "descricao")
	@OrderColumn(name = "posicao")
	private List<String> atividades = new ArrayList<>();

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getDescricao() {
		return descricao;
	}

	public void setDescricao(String descricao) {
		this.descricao = descricao;
	}

	public LocalDateTime getDataInicio() {
		return dataInicio;
	}

	public void setDataInicio(LocalDateTime dataInicio) {
		this.dataInicio = dataInicio;
	}

	public LocalDateTime getDataTermino() {
		return dataTermino;
	}

	public void setDataTermino(LocalDateTime dataTermino) {
		this.dataTermino = dataTermino;
	}

	public boolean getAtivo() {
		return ativo;
	}

	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}

	public String getFotoPerfil() {
		return fotoPerfil;
	}

	public void setFotoPerfil(String fotoPerfil) {
		this.fotoPerfil = fotoPerfil;
	}

	public String getBanner() {
		return banner;
	}

	public void setBanner(String banner) {
		this.banner = banner;
	}

	public List<String> getParticipantes() {
		return participantes;
	}

	public void setParticipantes(List<String> participantes) {
		this.participantes = participantes;
	}

	public List<String> getAtividades() {
		return atividades;
	}

	public void setAtividades(List<String> atividades) {
		this.atividades = atividades;
	}

	/**
	 * Getters auxiliares só para exibição/edição nas páginas Thymeleaf (não
	 * entram no JSON da API REST, graças ao @JsonIgnore).
	 */
	@JsonIgnore
	public String getDataInicioFormatada() {
		return dataInicio != null ? dataInicio.format(FORMATO_EXIBICAO) : "";
	}

	@JsonIgnore
	public String getDataTerminoFormatada() {
		return dataTermino != null ? dataTermino.format(FORMATO_EXIBICAO) : "";
	}

	@JsonIgnore
	public String getParticipantesTexto() {
		return participantes != null ? String.join("\n", participantes) : "";
	}

	@JsonIgnore
	public String getAtividadesTexto() {
		return atividades != null ? String.join("\n", atividades) : "";
	}
}
