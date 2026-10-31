package com.capricha.capricha_api.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.AvaliacaoAplicativo;
import com.capricha.capricha_api.entidade.Participacao;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.AvaliacaoAplicativoRepository;

import jakarta.annotation.PostConstruct;

@Service
public class AvaliacaoAplicativoService {
    private static final List<String> COLUNAS_LEGADAS = List.of(
            "navegacao",
            "clareza_informacoes",
            "utilidade_funcionalidades",
            "desempenho_tecnico",
            "recomendacao");

    private final AvaliacaoAplicativoRepository avaliacoes;

    @Autowired
    private ParticipacaoService participacaoService;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    public AvaliacaoAplicativoService(AvaliacaoAplicativoRepository avaliacoes) {
        this.avaliacoes = avaliacoes;
    }

    @PostConstruct
    public void removerColunasLegadas() {
        if (jdbcTemplate == null) {
            return;
        }
        for (String coluna : COLUNAS_LEGADAS) {
            try {
                jdbcTemplate.execute("ALTER TABLE avaliacao_aplicativo DROP COLUMN " + coluna);
            } catch (Exception ignored) {
                // Coluna já removida ou inexistente.
            }
        }
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

        avaliacao.setPeou01(respostas.peou01());
        avaliacao.setPeou02(respostas.peou02());
        avaliacao.setPeou03(respostas.peou03());
        avaliacao.setPeou04(respostas.peou04());
        avaliacao.setPeou05(respostas.peou05());
        avaliacao.setPeou06(respostas.peou06());

        avaliacao.setPu01(respostas.pu01());
        avaliacao.setPu02(respostas.pu02());
        avaliacao.setPu03(respostas.pu03());
        avaliacao.setPu04(respostas.pu04());
        avaliacao.setPu05(respostas.pu05());
        avaliacao.setPu06(respostas.pu06());

        avaliacao.setAc01(respostas.ac01());
        avaliacao.setAc02(respostas.ac02());
        avaliacao.setAc03(respostas.ac03());
        avaliacao.setAc04(respostas.ac04());
        avaliacao.setAc05(respostas.ac05());
        avaliacao.setAc06(respostas.ac06());
        avaliacao.setAc07(respostas.ac07());
        avaliacao.setAc08(respostas.ac08());

        avaliacao.setSe01(respostas.se01());
        avaliacao.setSe02(respostas.se02());
        avaliacao.setSe03(respostas.se03());
        avaliacao.setSe04(respostas.se04());
        avaliacao.setSe05(respostas.se05());
        avaliacao.setSe06(respostas.se06());
        avaliacao.setSe07(respostas.se07());
        avaliacao.setSe08(respostas.se08());

        avaliacao.setCom01(respostas.com01());
        avaliacao.setCom02(respostas.com02());
        avaliacao.setCom03(respostas.com03());

        avaliacao.setCoo01(respostas.coo01());
        avaliacao.setCoo02(respostas.coo02());
        avaliacao.setCoo03(respostas.coo03());

        avaliacao.setCop01(respostas.cop01());
        avaliacao.setCop02(respostas.cop02());
        avaliacao.setCop03(respostas.cop03());

        String comentario = respostas.comentario() == null ? null : respostas.comentario().trim();
        avaliacao.setComentario(comentario == null || comentario.isEmpty() ? null : comentario);
        avaliacao.setRespondidaEm(Instant.now());

        List<Participacao> participacoes = participacaoService.getParticipacoesByUsuario(usuario.getId());

        for (Participacao participacao : participacoes) {
            participacaoService.atualizarScore(usuario.getId(), participacao.getEvento().getId(), 200);
        }

        return avaliacoes.save(avaliacao);
    }

    private void validarParticipante(Usuario usuario) {
        if (usuario == null || !"PARTICIPANTE".equalsIgnoreCase(usuario.getPerfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A avaliação está disponível somente para participantes.");
        }
    }

    public record Respostas(
            Integer peou01, Integer peou02, Integer peou03, Integer peou04, Integer peou05, Integer peou06,
            Integer pu01, Integer pu02, Integer pu03, Integer pu04, Integer pu05, Integer pu06,
            Integer ac01, Integer ac02, Integer ac03, Integer ac04, Integer ac05, Integer ac06, Integer ac07, Integer ac08,
            Integer se01, Integer se02, Integer se03, Integer se04, Integer se05, Integer se06, Integer se07, Integer se08,
            Integer com01, Integer com02, Integer com03,
            Integer coo01, Integer coo02, Integer coo03,
            Integer cop01, Integer cop02, Integer cop03,
            String comentario) {}
}
