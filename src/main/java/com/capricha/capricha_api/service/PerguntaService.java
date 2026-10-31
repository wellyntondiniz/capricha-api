package com.capricha.capricha_api.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.capricha.capricha_api.repository.PerguntaRepository;

import jakarta.transaction.Transactional;

import com.capricha.capricha_api.entidade.Alternativa;
import com.capricha.capricha_api.entidade.Pergunta;

@Service
public class PerguntaService {

	public static final int ENUNCIADO_MINIMO = 5;
	public static final int TEXTO_MAXIMO = 255;
	public static final int ALTERNATIVAS_MINIMO = 2;

	@Autowired
	private PerguntaRepository perguntaRepository;

	public List<Pergunta> getPerguntas() {
		return perguntaRepository.findAllByAtivo(true);
	}

	public Pergunta getPerguntaById(Integer id) {
		return perguntaRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pergunta não encontrada."));
	}

	public Pergunta salvar(Pergunta pergunta) {
		return perguntaRepository.save(pergunta);
	}

	/**
	 * Edita o enunciado e as alternativas. Alternativas com id são atualizadas, as sem id são criadas e as que
	 * não vieram na lista são removidas. A palestra da pergunta não muda por aqui: o quiz depende dela.
	 */
	@Transactional
	public Pergunta updatePergunta(Integer id, Pergunta dados) {
		Pergunta pergunta = getPerguntaById(id);

		String enunciado = dados.getEnunciado() == null ? "" : dados.getEnunciado().trim();
		if (enunciado.isEmpty()) {
			throw invalido("Digite o enunciado da pergunta.");
		}
		if (enunciado.length() < ENUNCIADO_MINIMO || enunciado.length() > TEXTO_MAXIMO) {
			throw invalido("O enunciado deve ter entre " + ENUNCIADO_MINIMO + " e " + TEXTO_MAXIMO + " caracteres.");
		}

		List<Alternativa> recebidas = dados.getAlternativas();
		if (recebidas != null) {
			validarAlternativas(recebidas);
		}

		pergunta.setEnunciado(enunciado);

		if (recebidas != null) {
			List<Alternativa> atuais = pergunta.getAlternativas();

			atuais.removeIf(atual -> recebidas.stream()
					.noneMatch(recebida -> recebida.getId() != null && recebida.getId().equals(atual.getId())));

			for (Alternativa recebida : recebidas) {
				String texto = recebida.getTexto().trim();
				if (recebida.getId() == null) {
					recebida.setTexto(texto);
					recebida.setPergunta(pergunta); // sem isso a nova alternativa ficaria sem pergunta_id
					pergunta.adicionarAlternativa(recebida);
				} else {
					Alternativa atual = atuais.stream()
							.filter(a -> a.getId().equals(recebida.getId()))
							.findFirst()
							.orElseThrow(() -> invalido("A alternativa não pertence a esta pergunta."));
					atual.setTexto(texto);
					atual.setCorreta(recebida.getCorreta());
				}
			}
		}

		return perguntaRepository.save(pergunta);
	}

	public Pergunta deletar(Integer id) {
		Pergunta pergunta = getPerguntaById(id);
		pergunta.setAtivo(false);
		return perguntaRepository.save(pergunta);
	}

	/** Mesmas regras do cadastro: 2+ alternativas, sem texto vazio ou repetido, ao menos uma certa e uma errada. */
	private void validarAlternativas(List<Alternativa> alternativas) {
		if (alternativas.size() < ALTERNATIVAS_MINIMO) {
			throw invalido("A pergunta precisa de pelo menos " + ALTERNATIVAS_MINIMO + " alternativas.");
		}
		Set<String> textos = new HashSet<>();
		int corretas = 0;
		for (Alternativa alternativa : alternativas) {
			String texto = alternativa.getTexto() == null ? "" : alternativa.getTexto().trim();
			if (texto.isEmpty()) {
				throw invalido("Preencha o texto de todas as alternativas.");
			}
			if (texto.length() > TEXTO_MAXIMO) {
				throw invalido("Cada alternativa deve ter no máximo " + TEXTO_MAXIMO + " caracteres.");
			}
			if (!textos.add(texto.toLowerCase())) {
				throw invalido("As alternativas não podem se repetir.");
			}
			if (alternativa.getCorreta()) {
				corretas++;
			}
		}
		if (corretas == 0) {
			throw invalido("Selecione pelo menos uma alternativa correta.");
		}
		if (corretas == alternativas.size()) {
			throw invalido("Pelo menos uma alternativa precisa ser incorreta.");
		}
	}

	private ResponseStatusException invalido(String mensagem) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
	}
}