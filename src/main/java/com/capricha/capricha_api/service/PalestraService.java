package com.capricha.capricha_api.service;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Pergunta;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.PalestraRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PalestraService {

    private final PalestraRepository palestraRepository;

    public PalestraService(PalestraRepository palestraRepository) {
        this.palestraRepository = palestraRepository;
    }

    public Palestra salvar(Palestra palestra) {
        return palestraRepository.save(palestra);
    }

    public List<Palestra> listar() {
        return palestraRepository.findAll();
    }
    
    public Pergunta adicionarPergunta(Long id, Pergunta pergunta) {
    	Palestra palestra = palestraRepository.getById(id);
    	if (palestra == null) {
    		throw new RuntimeException("Palestra não encontrada");
    	}
    	
    	palestra.adicionarPergunta(pergunta);
    	
    	for (Alternativa alternativa : pergunta.getAlternativas()) {
            alternativa.setPergunta(pergunta);
        }
    	
    	palestra = palestraRepository.save(palestra);
    	
    	return palestra.getPerguntas().get(palestra.getPerguntas().size() - 1);		
    }
    
    public List<Pergunta> listarPerguntas(Long id) {
    	Palestra palestra = palestraRepository.getById(id);
    	if (palestra == null) {
    		new RuntimeException("Palestra não encontrada");
    	}
    	
    	return palestra.getPerguntas();
    }
}