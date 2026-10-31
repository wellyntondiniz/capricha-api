package com.capricha.capricha_api.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.AvaliacaoAplicativo;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.AvaliacaoAplicativoRepository;

@Service
public class AvaliacaoAplicativoService {
    private final AvaliacaoAplicativoRepository avaliacoes;

    public AvaliacaoAplicativoService(AvaliacaoAplicativoRepository avaliacoes) {
        this.avaliacoes = avaliacoes;
    }

    @Transactional(readOnly = true)
    public boolean jaRespondeu(Usuario usuario) {
        validarParticipante(usuario);
        return avaliacoes.existsByUsuarioId(usuario.getId());
    }

    @Transactional
    public AvaliacaoAplicativo salvar(Usuario usuario, Respostas respostas) {
        validarParticipante(usuario);
        if (avaliacoes.existsByUsuarioId(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já enviou a avaliação do aplicativo.");
        }

        var avaliacao = new AvaliacaoAplicativo();
        avaliacao.setUsuario(usuario);
        avaliacao.setNavegacao(respostas.navegacao());
        avaliacao.setClarezaInformacoes(respostas.clarezaInformacoes());
        avaliacao.setUtilidadeFuncionalidades(respostas.utilidadeFuncionalidades());
        avaliacao.setDesempenhoTecnico(respostas.desempenhoTecnico());
        avaliacao.setRecomendacao(respostas.recomendacao());
        String comentario = respostas.comentario() == null ? null : respostas.comentario().trim();
        avaliacao.setComentario(comentario == null || comentario.isEmpty() ? null : comentario);
        avaliacao.setRespondidaEm(Instant.now());
        return avaliacoes.save(avaliacao);
    }

    private void validarParticipante(Usuario usuario) {
        if (usuario == null || !"PARTICIPANTE".equalsIgnoreCase(usuario.getPerfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A avaliação está disponível somente para participantes.");
        }
    }

    public record Respostas(Integer navegacao, Integer clarezaInformacoes,
            Integer utilidadeFuncionalidades, Integer desempenhoTecnico,
            Integer recomendacao, String comentario) {}
}
