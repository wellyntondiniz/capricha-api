package com.capricha.capricha_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// Cada usuário responde um quiz uma única vez; a restrição única barra também envios duplicados simultâneos
@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_resposta_quiz_usuario", columnNames = {"usuario_id", "quiz_id"}))
public class RespostaQuiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "quiz_id")
    private Long quizId;

    private Integer acertos;

    private Integer totalPerguntas;

    private Integer pontosObtidos;

    private LocalDateTime dataResposta;

    public RespostaQuiz() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Integer usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Integer getAcertos() {
        return acertos;
    }

    public void setAcertos(Integer acertos) {
        this.acertos = acertos;
    }

    public Integer getTotalPerguntas() {
        return totalPerguntas;
    }

    public void setTotalPerguntas(Integer totalPerguntas) {
        this.totalPerguntas = totalPerguntas;
    }

    public Integer getPontosObtidos() {
        return pontosObtidos;
    }

    public void setPontosObtidos(Integer pontosObtidos) {
        this.pontosObtidos = pontosObtidos;
    }

    public LocalDateTime getDataResposta() {
        return dataResposta;
    }

    public void setDataResposta(LocalDateTime dataResposta) {
        this.dataResposta = dataResposta;
    }
}
