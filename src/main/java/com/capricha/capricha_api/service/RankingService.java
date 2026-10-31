package com.capricha.capricha_api.service;

import com.capricha.capricha_api.dto.PontuacaoRankingRequest;
import com.capricha.capricha_api.model.PontuacaoRanking;
import com.capricha.capricha_api.repository.PontuacaoRankingRepository;
import com.capricha.capricha_api.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class RankingService {

    private final PontuacaoRankingRepository pontuacaoRankingRepository;
    private final UsuarioRepository usuarioRepository;

    public RankingService(PontuacaoRankingRepository pontuacaoRankingRepository, UsuarioRepository usuarioRepository) {
        this.pontuacaoRankingRepository = pontuacaoRankingRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public PontuacaoRanking adicionarPontos(PontuacaoRankingRequest request) {
        if (request == null || request.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o usuário que vai receber os pontos.");
        }
        if (request.pontos() == null || request.pontos() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A pontuação precisa ser maior que zero.");
        }
        if (!usuarioRepository.existsById(request.usuarioId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado.");
        }

        PontuacaoRanking pontuacao = new PontuacaoRanking();
        pontuacao.setUsuarioId(request.usuarioId());
        pontuacao.setPontos(request.pontos());
        pontuacao.setOrigem(request.origem());
        pontuacao.setDataRegistro(LocalDateTime.now());
        return pontuacaoRankingRepository.save(pontuacao);
    }
}
