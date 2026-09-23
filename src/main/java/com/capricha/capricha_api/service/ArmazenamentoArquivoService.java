package com.capricha.capricha_api.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Responsável só por salvar imagens enviadas (foto de perfil / banner do
 * evento) em disco e devolver o nome do arquivo gerado. Não mexe em nenhum
 * outro service existente.
 */
@Service
public class ArmazenamentoArquivoService {

	private final Path diretorioBase;

	public ArmazenamentoArquivoService(@Value("${app.upload-dir:uploads/eventos}") String diretorio) {
		this.diretorioBase = Paths.get(diretorio).toAbsolutePath().normalize();
		try {
			Files.createDirectories(diretorioBase);
		} catch (IOException e) {
			throw new IllegalStateException("Não foi possível criar a pasta de uploads: " + diretorioBase, e);
		}
	}

	/**
	 * Salva o arquivo enviado com um nome único e devolve esse nome. Se
	 * nenhum arquivo tiver sido enviado (campo vazio), devolve null.
	 */
	public String salvar(MultipartFile arquivo) {
		if (arquivo == null || arquivo.isEmpty()) {
			return null;
		}

		String nomeArquivo = UUID.randomUUID() + extrairExtensao(arquivo.getOriginalFilename());

		try {
			Path destino = diretorioBase.resolve(nomeArquivo);
			Files.copy(arquivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new ResponseStatusException(
					HttpStatus.INTERNAL_SERVER_ERROR,
					"Não foi possível salvar a imagem enviada."
			);
		}

		return nomeArquivo;
	}

	private String extrairExtensao(String nomeOriginal) {
		if (nomeOriginal == null || !nomeOriginal.contains(".")) {
			return "";
		}
		return nomeOriginal.substring(nomeOriginal.lastIndexOf('.'));
	}
}
