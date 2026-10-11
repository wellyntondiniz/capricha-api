package com.capricha.capricha_api.config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Value("${app.upload-dir:uploads/eventos}")
	private String diretorioUpload;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String caminhoAbsoluto = Paths.get(diretorioUpload).toAbsolutePath().normalize().toUri().toString();
		registry.addResourceHandler("/imagens/eventos/**")
				.addResourceLocations(caminhoAbsoluto);
	}
}
