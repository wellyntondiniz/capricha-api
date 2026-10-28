package com.capricha.capricha_api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.capricha.capricha_api.entidade.SessaoUsuario;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.SessaoUsuarioRepository;

@Service
public class SessaoUsuarioService {
    private final SessaoUsuarioRepository sessoes;
    private final Duration validade;
    private final SecureRandom random = new SecureRandom();

    public SessaoUsuarioService(SessaoUsuarioRepository sessoes,
            @Value("${app.auth.session-hours:8}") long horas) {
        this.sessoes = sessoes;
        this.validade = Duration.ofHours(Math.max(1, horas));
    }

    public record Credencial(String token, Instant expiraEm) {}

    @Transactional
    public Credencial emitir(Usuario usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiraEm = Instant.now().plus(validade);
        sessoes.save(new SessaoUsuario(hash(token), usuario, expiraEm, hash(usuario.getSenha())));
        return new Credencial(token, expiraEm);
    }

    @Transactional(readOnly = true)
    public Usuario autenticar(String authorization) {
        String token = extrairToken(authorization);
        SessaoUsuario sessao = sessoes.findById(hash(token)).orElseThrow(this::naoAutorizado);
        Usuario usuario = sessao.getUsuario();
        if (!sessao.getExpiraEm().isAfter(Instant.now()) || !usuario.getAtivo()
                || !MessageDigest.isEqual(sessao.getSenhaFingerprint().getBytes(StandardCharsets.UTF_8),
                        hash(usuario.getSenha()).getBytes(StandardCharsets.UTF_8))) {
            throw naoAutorizado();
        }
        return usuario;
    }

    @Transactional
    public void encerrar(String authorization) {
        String token = extrairToken(authorization);
        sessoes.findById(hash(token)).ifPresent(sessoes::delete);
    }

    private String extrairToken(String authorization) {
        if (authorization == null || !authorization.matches("Bearer [A-Za-z0-9_-]{43}")) {
            throw naoAutorizado();
        }
        return authorization.substring(7);
    }

    private ResponseStatusException naoAutorizado() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Sua sessão expirou. Faça login novamente.");
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
