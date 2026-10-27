package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.capricha.capricha_api.entidade.PerguntaAvaliacaoAplicativo;
import com.capricha.capricha_api.repository.PerguntaAvaliacaoAplicativoRepository;

@Component
public class CargaInicialAvaliacaoAplicativo implements ApplicationRunner {
    private static final List<String> PERGUNTAS = List.of(
            "O aplicativo foi fácil de usar?",
            "As informações apresentadas estavam claras?",
            "As funcionalidades ajudaram na sua experiência no evento?",
            "O aplicativo apresentou bom desempenho durante o uso?",
            "Você recomendaria este aplicativo para outros participantes?");

    private final PerguntaAvaliacaoAplicativoRepository repository;

    public CargaInicialAvaliacaoAplicativo(PerguntaAvaliacaoAplicativoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (int indice = 0; indice < PERGUNTAS.size(); indice++) {
            String texto = PERGUNTAS.get(indice);
            if (repository.existsByTexto(texto)) continue;
            var pergunta = new PerguntaAvaliacaoAplicativo();
            pergunta.setTexto(texto);
            pergunta.setOrdem(indice + 1);
            pergunta.setAtiva(true);
            repository.save(pergunta);
        }
    }
}
