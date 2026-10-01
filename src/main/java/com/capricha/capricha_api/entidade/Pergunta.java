package com.capricha.capricha_api.entidade;

import java.util.ArrayList;
import java.util.List;

import com.capricha.capricha_api.model.Palestra;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pergunta")
public class Pergunta {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	@Column(name = "enunciado")
	private String enunciado;
	
	@ManyToOne
	@JoinColumn(name = "palestra_id", nullable = false)
	private Palestra palestra;
	
	@OneToMany(
        mappedBy = "pergunta",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Alternativa> alternativas = new ArrayList<>();
	
	@Column(name = "ativo")
	private boolean ativo = true;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public String getEnunciado() {
		return enunciado;
	}
	
	public void setEnunciado(String enunciado) {
		this.enunciado = enunciado;
	}
	
	public Palestra getPalestra() {
		return palestra;
	}
	
	public void setPalestra(Palestra palestra) {
		this.palestra = palestra;
	}
	
	public boolean getAtivo() {
		return ativo;
	}
	
	public void setAtivo(boolean ativo) {
		this.ativo = ativo;
	}
	
	public List<Alternativa> getAlternativas() {
        return alternativas;
    }

    public void adicionarAlternativa(Alternativa alternativa) {
        alternativas.add(alternativa);
        alternativa.setPergunta(this);
    }
    
}
