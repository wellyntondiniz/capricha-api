package com.capricha.capricha_api.model;

import java.util.ArrayList;
import java.util.List;

import com.capricha.capricha_api.entidade.Pergunta;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class Palestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String descricao;

    private String palestrante;

    private String data;

    private String horario;

    private String evento;
    
    @OneToMany(
        mappedBy = "palestra",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<Pergunta> perguntas;

    public Palestra() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getPalestrante() {
        return palestrante;
    }

    public void setPalestrante(String palestrante) {
        this.palestrante = palestrante;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }
    
    public List<Pergunta> getPerguntas() {
    	return perguntas;
    }
    
    public void adicionarPergunta(Pergunta pergunta) {
    	perguntas.add(pergunta);
    	pergunta.setPalestra(this);
    }
}