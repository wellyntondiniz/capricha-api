package com.capricha.capricha_api.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Senhas {
    private Senhas() {}
    public static String proteger(String senha) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2$600000$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derivar(senha, salt, 600000));
    }
    public static boolean confere(String senha, String armazenada) {
        if (armazenada == null) return false;
        // Compatibilidade com contas antigas da CA-01; novas senhas sempre usam hash.
        if (!armazenada.startsWith("pbkdf2$")) return MessageDigest.isEqual(
                senha.getBytes(StandardCharsets.UTF_8), armazenada.getBytes(StandardCharsets.UTF_8));
        try {
            String[] partes = armazenada.split("\\$");
            return MessageDigest.isEqual(Base64.getDecoder().decode(partes[3]),
                    derivar(senha, Base64.getDecoder().decode(partes[2]), Integer.parseInt(partes[1])));
        } catch (RuntimeException erro) { return false; }
    }
    private static byte[] derivar(String senha, byte[] salt, int iteracoes) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(), salt, iteracoes, 256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch (Exception erro) { throw new IllegalStateException("Falha ao proteger senha", erro); }
        finally { spec.clearPassword(); }
    }
}
