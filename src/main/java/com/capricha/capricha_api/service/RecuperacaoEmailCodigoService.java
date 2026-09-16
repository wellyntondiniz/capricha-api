package com.capricha.capricha_api.service;

import com.capricha.capricha_api.entidade.CodigoEmail;
import com.capricha.capricha_api.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@Service
public class RecuperacaoEmailCodigoService {
    private final CodigoEmailRepository codigos;
    private final RecuperacaoUsuarioRepository usuarios;
    private final EnvioCodigoEmail envio;
    private final SecureRandom random = new SecureRandom();
    public RecuperacaoEmailCodigoService(CodigoEmailRepository codigos,
            RecuperacaoUsuarioRepository usuarios, EnvioCodigoEmail envio) {
        this.codigos = codigos; this.usuarios = usuarios; this.envio = envio;
    }
    @Transactional
    public Map<String,Object> solicitar(String email) {
        envio.validar();
        String id = UUID.randomUUID().toString();
        String normalizado = email.trim().toLowerCase(Locale.ROOT);
        var usuario = usuarios.buscarAtivoParaAtualizacao(normalizado);
        Instant agora = Instant.now();
        if (usuario.isPresent()) {
            var ultimo = codigos.findFirstByEmailOrderByCriadoDesc(normalizado);
            if (ultimo.filter(c -> c.criado.plusSeconds(60).isAfter(agora)).isEmpty()
                    && codigos.countByEmailAndCriadoAfter(normalizado, agora.minusSeconds(900)) < 3) {
                String codigo = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
                CodigoEmail desafio = new CodigoEmail();
                desafio.id=id; desafio.email=normalizado; desafio.hash=hash(id+":"+codigo);
                desafio.criado=agora; desafio.expira=agora.plusSeconds(600);
                codigos.save(desafio); envio.enviar(normalizado,codigo,id);
            }
        }
        return Map.of("desafio",id,"simulado",envio.simulado(),"mensagem",
            envio.simulado() ? "Modo local: consulte o código de e-mail simulado no terminal da API."
            : "Se o e-mail estiver cadastrado, enviaremos um código.");
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void validar(String id,String codigo) {
        CodigoEmail c=obter(id);
        if(c.validado)return;
        c.tentativas++; codigos.save(c);
        if(!MessageDigest.isEqual(c.hash.getBytes(StandardCharsets.UTF_8),
                hash(id+":"+codigo).getBytes(StandardCharsets.UTF_8))) throw invalido();
        c.validado=true; codigos.save(c);
    }
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public void redefinir(String id,String codigo,String novaSenha) {
        String email=codigos.findById(id).map(c->c.email).orElseThrow(this::invalido);
        var usuario=usuarios.buscarAtivoParaAtualizacao(email).orElseThrow(this::invalido);
        CodigoEmail c=obter(id);
        if(!c.validado) validar(id,codigo);
        else if(!MessageDigest.isEqual(c.hash.getBytes(StandardCharsets.UTF_8),
                hash(id+":"+codigo).getBytes(StandardCharsets.UTF_8))) throw invalido();
        if(Senhas.confere(novaSenha,usuario.getSenha()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A nova senha não pode ser igual à anterior.");
        usuario.setSenha(Senhas.proteger(novaSenha)); usuarios.save(usuario);
        codigos.invalidar(email);
    }
    private CodigoEmail obter(String id){
        CodigoEmail c=codigos.findById(id).orElseThrow(this::invalido);
        if(c.utilizado||c.tentativas>=5||!c.expira.isAfter(Instant.now()))throw invalido();
        return c;
    }
    private ResponseStatusException invalido(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Código inválido, expirado, já utilizado ou limite de tentativas atingido.");}
    private String hash(String v){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
        .digest(v.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
