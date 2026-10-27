package com.capricha.capricha_api.rest;

import java.util.List;

import com.capricha.capricha_api.entidade.Resumo;

public record ResumoResponse(
		Integer id,
		String titulo,
		String texto,
		List<String> autores,
		String banner,
		List<String> palavrasChave,
		Integer eventoId,
		String eventoNome) {

	public static ResumoResponse de(Resumo resumo) {
		return new ResumoResponse(
				resumo.getId(),
				resumo.getTitulo(),
				resumo.getTexto(),
				List.copyOf(resumo.getAutores()),
				resumo.getBanner(),
				List.copyOf(resumo.getPalavrasChave()),
				resumo.getEvento().getId(),
				resumo.getEvento().getNome());
	}
}
