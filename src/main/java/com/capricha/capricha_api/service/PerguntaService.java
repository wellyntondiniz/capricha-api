package com.capricha.capricha_api.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capricha.capricha_api.repository.PerguntaRepository;

import jakarta.transaction.Transactional;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Pergunta;

@Service
public class PerguntaService {
	
	@Autowired
	private PerguntaRepository perguntaRepository;
	
	public List<Pergunta> getPerguntas() {
		return perguntaRepository.findAllByAtivo(true);
	}
	
	public Pergunta getPerguntaById(Integer id) {
		Pergunta pergunta= perguntaRepository.findById(id).get();
		return pergunta;
	}
	
	public Pergunta salvar(Pergunta pergunta) {
		return perguntaRepository.save(pergunta);
	}
	
	@Transactional
	public Pergunta updatePergunta(Integer id, Pergunta dados) {
	    Pergunta pergunta = perguntaRepository.findById(id)
	        .orElseThrow(() ->
	            new RuntimeException("Pergunta não encontrada: " + id)
	        );

	    pergunta.setEnunciado(dados.getEnunciado());

	    if (dados.getPalestra() != null) {
	        pergunta.setPalestra(dados.getPalestra());
	    }

	    List<Alternativa> atuais = pergunta.getAlternativas();
	    List<Alternativa> recebidas = dados.getAlternativas();

	    if (recebidas != null) {
	        atuais.removeIf(atual ->
	            recebidas.stream().noneMatch(recebida ->
	                recebida.getId() != null
	                    && recebida.getId().equals(atual.getId())
	            )
	        );

	        for (Alternativa recebida : recebidas) {
	            if (recebida.getId() == null) {
	                pergunta.adicionarAlternativa(recebida);
	            } else {
	                Alternativa atual = atuais.stream()
	                    .filter(a -> a.getId().equals(recebida.getId()))
	                    .findFirst()
	                    .orElseThrow(() ->
	                        new RuntimeException(
	                            "Alternativa não pertence a esta pergunta"
	                        )
	                    );

	                atual.setTexto(recebida.getTexto());
	                atual.setCorreta(recebida.getCorreta());
	            }
	        }
	    }

	    return perguntaRepository.save(pergunta);
	}
	
	public Pergunta deletar(Integer id) {
		Pergunta evento = getPerguntaById(id);
		evento.setAtivo(false);
		return perguntaRepository.save(evento);
	}
}
