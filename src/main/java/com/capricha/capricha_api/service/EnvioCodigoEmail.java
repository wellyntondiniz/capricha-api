package com.capricha.capricha_api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.*;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@org.springframework.context.annotation.PropertySources({
    @org.springframework.context.annotation.PropertySource("classpath:recuperacao-email.properties"),
    @org.springframework.context.annotation.PropertySource(value="file:./application-secrets.properties", ignoreResourceNotFound=true)
})
public class EnvioCodigoEmail {
    private static final Logger LOG = LoggerFactory.getLogger(EnvioCodigoEmail.class);

    private static final String MENSAGEM_INDISPONIVEL =
        "Não foi possível enviar o código no momento. Tente novamente em alguns minutos.";
    private final JavaMailSender mail;
    private final Environment ambiente;
    private final String modo, remetente, senha;
    public EnvioCodigoEmail(JavaMailSender mail, Environment ambiente,
            @Value("${app.email.mode:smtp}") String modo,
            @Value("${spring.mail.username:}") String remetente,
            @Value("${spring.mail.password:}") String senha) {
        this.mail = mail; this.ambiente = ambiente; this.modo = modo;
        this.remetente = remetente; this.senha = senha;
    }
    public boolean simulado() {
        return modo.equals("console") && ambiente.acceptsProfiles(Profiles.of("local"));
    }
    public void validar() {
        if (!simulado() && (remetente.isBlank() || senha.isBlank())) {
            LOG.error("Envio de e-mail não configurado. Defina MAIL_USERNAME e MAIL_APP_PASSWORD.");
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_INDISPONIVEL);
        }
    }
    public void enviar(String destinatario, String codigo, String desafio) {
        validar();
        if (simulado()) {
            LOG.info("[E-MAIL SIMULADO - NÃO ENVIADO] Desafio {} | Código: {} | Validade: 10 minutos",
                desafio, codigo);
            return;
        }
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente); mensagem.setTo(destinatario);
        mensagem.setSubject("Código de recuperação de senha - Capricha");
        mensagem.setText("Seu código para redefinir a senha é: " + codigo
            + "\n\nEle é válido por 10 minutos. Se você não solicitou, ignore.");
        try { mail.send(mensagem); }
        catch (org.springframework.mail.MailException erro) {
            LOG.error("Falha ao enviar e-mail de recuperação. Verifique a senha de aplicativo do Gmail.", erro);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, MENSAGEM_INDISPONIVEL);
        }
    }
}