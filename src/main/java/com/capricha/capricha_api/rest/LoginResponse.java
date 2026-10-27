package com.capricha.capricha_api.rest;

import java.time.Instant;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.service.SessaoUsuarioService.Credencial;

/** Mantém os campos do login principal e acrescenta sua credencial de sessão. */
public record LoginResponse(Integer id, String nome, String email, boolean ativo, String perfil,
        String foto, String token, Instant expiraEm) {
    @Override
    public String toString() { return "LoginResponse[id=" + id + ", token=[protegido]]"; }

    public static LoginResponse de(Usuario usuario, Credencial credencial) {
        UsuarioResponse dados = UsuarioResponse.de(usuario);
        return new LoginResponse(dados.id(), dados.nome(), dados.email(), dados.ativo(), dados.perfil(),
                dados.foto(), credencial.token(), credencial.expiraEm());
    }
}
