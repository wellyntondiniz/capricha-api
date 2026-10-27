package com.capricha.capricha_api.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.entidade.PerguntaAvaliacaoAplicativo;
import com.capricha.capricha_api.entidade.RespostaAvaliacaoAplicativo;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.PerguntaAvaliacaoAplicativoRepository;
import com.capricha.capricha_api.repository.RespostaAvaliacaoAplicativoRepository;

@Service
public class AvaliacaoAplicativoService {
    private final PerguntaAvaliacaoAplicativoRepository perguntas;
    private final RespostaAvaliacaoAplicativoRepository respostas;

    public AvaliacaoAplicativoService(PerguntaAvaliacaoAplicativoRepository perguntas,
            RespostaAvaliacaoAplicativoRepository respostas) {
        this.perguntas = perguntas;
        this.respostas = respostas;
    }

    @Transactional(readOnly = true)
    public List<PerguntaAvaliacaoAplicativo> listarPerguntas() {
        return perguntas.findByAtivaTrueOrderByOrdemAscIdAsc();
    }

    @Transactional(readOnly = true)
    public boolean jaRespondeu(Usuario usuario) {
        validarParticipante(usuario);
        return respostas.existsByUsuarioId(usuario.getId());
    }

    @Transactional
    public void responder(Usuario usuario, List<RespostaRecebida> recebidas) {
        validarParticipante(usuario);
        if (respostas.existsByUsuarioId(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Você já enviou a avaliação do aplicativo.");
        }

        List<PerguntaAvaliacaoAplicativo> ativas = listarPerguntas();
        if (ativas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não há perguntas de avaliação disponíveis.");
        }
        if (recebidas == null || recebidas.size() != ativas.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Responda todas as perguntas da avaliação.");
        }

        var idsAtivos = ativas.stream().map(PerguntaAvaliacaoAplicativo::getId).collect(java.util.stream.Collectors.toSet());
        var idsRecebidos = new HashSet<Integer>();
        Instant agora = Instant.now();
        for (RespostaRecebida recebida : recebidas) {
            if (recebida == null || recebida.perguntaId() == null || recebida.nota() == null
                    || recebida.nota() < 1 || recebida.nota() > 5
                    || !idsAtivos.contains(recebida.perguntaId())
                    || !idsRecebidos.add(recebida.perguntaId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "As respostas da avaliação são inválidas.");
            }
            var resposta = new RespostaAvaliacaoAplicativo();
            resposta.setUsuario(usuario);
            resposta.setPergunta(perguntas.getReferenceById(recebida.perguntaId()));
            resposta.setNota(recebida.nota());
            resposta.setRespondidaEm(agora);
            respostas.save(resposta);
        }
    }

    private void validarParticipante(Usuario usuario) {
        if (usuario == null || !"PARTICIPANTE".equalsIgnoreCase(usuario.getPerfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "A avaliação está disponível somente para participantes.");
        }
    }

    public record RespostaRecebida(Integer perguntaId, Integer nota) {}
}
