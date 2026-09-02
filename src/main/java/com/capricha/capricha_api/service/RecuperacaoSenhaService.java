package com.capricha.capricha_api.service;

import com.capricha.capricha_api.entidade.RecuperacaoSenha;
import com.capricha.capricha_api.repository.RecuperacaoSenhaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

@Service
public class RecuperacaoSenhaService {
    private final RecuperacaoSenhaRepository repository;
    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;
    private final String remetente;
    private final String urlRecuperacao;

    public RecuperacaoSenhaService(RecuperacaoSenhaRepository repository,
            JdbcTemplate jdbcTemplate,
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String remetente,
            @Value("${app.recuperacao.url}") String urlRecuperacao) {
        this.repository = repository;
        this.jdbcTemplate = jdbcTemplate;
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.urlRecuperacao = urlRecuperacao;
    }

    public String solicitar(String email) {
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        Integer quantidade = jdbcTemplate.queryForObject(
                "select count(*) from usuario where lower(email) = ? and ativo = true",
                Integer.class, normalizado);
        if (quantidade == null || quantidade == 0) return null;

        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        RecuperacaoSenha recuperacao = new RecuperacaoSenha();
        recuperacao.setEmail(normalizado);
        recuperacao.setTokenHash(hash(token));
        recuperacao.setExpiraEm(LocalDateTime.now().plusMinutes(15));
        recuperacao.setUtilizado(false);
        repository.save(recuperacao);
        enviarLink(normalizado, token);
        return token;
    }

    private void enviarLink(String email, String token) {
        String separador = urlRecuperacao.contains("?") ? "&" : "?";
        String link = urlRecuperacao + separador + "token=" + token;
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Recuperação de senha - Capricha");
        mensagem.setText("Recebemos uma solicitação para redefinir sua senha.\n\n"
                + "Abra o link abaixo em até 15 minutos:\n" + link
                + "\n\nSe você não solicitou a alteração, ignore esta mensagem.");
        mailSender.send(mensagem);
    }

    @Transactional
    public ResultadoRedefinicao redefinir(String token, String novaSenha) {
        RecuperacaoSenha recuperacao = repository.findByTokenHashAndUtilizadoFalse(hash(token))
                .filter(item -> item.getExpiraEm().isAfter(LocalDateTime.now()))
                .orElse(null);
        if (recuperacao == null) return ResultadoRedefinicao.TOKEN_INVALIDO;

        List<String> senhasAtuais = jdbcTemplate.query(
                "select senha from usuario where lower(email) = ? and ativo = true",
                (resultado, linha) -> resultado.getString("senha"), recuperacao.getEmail());
        if (senhasAtuais.isEmpty()) return ResultadoRedefinicao.USUARIO_NAO_ENCONTRADO;
        if (senhasAtuais.get(0).equals(novaSenha)) return ResultadoRedefinicao.SENHA_REPETIDA;

        int alterados = jdbcTemplate.update(
                "update usuario set senha = ? where lower(email) = ? and ativo = true",
                novaSenha, recuperacao.getEmail());
        if (alterados == 0) return ResultadoRedefinicao.USUARIO_NAO_ENCONTRADO;
        recuperacao.setUtilizado(true);
        repository.save(recuperacao);
        return ResultadoRedefinicao.SUCESSO;
    }

    private static String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao proteger token", exception);
        }
    }

    public enum ResultadoRedefinicao {
        SUCESSO, TOKEN_INVALIDO, USUARIO_NAO_ENCONTRADO, SENHA_REPETIDA
    }
}
