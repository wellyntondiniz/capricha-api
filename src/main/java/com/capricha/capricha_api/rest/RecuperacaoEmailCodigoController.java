package com.capricha.capricha_api.rest;

import com.capricha.capricha_api.service.RecuperacaoEmailCodigoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/auth/email") @CrossOrigin
public class RecuperacaoEmailCodigoController {
    private final RecuperacaoEmailCodigoService service;
    public RecuperacaoEmailCodigoController(RecuperacaoEmailCodigoService service){this.service=service;}
    @PostMapping("/solicitar") public Map<String,Object> solicitar(@Valid @RequestBody Solicitar p){
        return service.solicitar(p.email());}
    @PostMapping("/validar-codigo") public Map<String,String> validar(@Valid @RequestBody Codigo p){
        service.validar(p.desafio(),p.codigo());return Map.of("mensagem","Código válido.");}
    @PostMapping("/redefinir-senha") public Map<String,String> redefinir(@Valid @RequestBody Redefinir p){
        service.redefinir(p.desafio(),p.codigo(),p.novaSenha());
        return Map.of("mensagem","Senha alterada com sucesso.");}
    public record Solicitar(@NotBlank(message="Informe o e-mail.") @Email(message="Digite um e-mail válido.")
        @Size(max=254,message="O e-mail deve ter no máximo 254 caracteres.") String email){}
    public record Codigo(@NotBlank(message="Solicitação de recuperação inválida.") @Size(max=36,message="Solicitação de recuperação inválida.") String desafio,
        @NotNull(message="Informe o código.") @Pattern(regexp="[0-9]{6}",message="O código deve ter 6 dígitos.") String codigo){}
    public record Redefinir(@NotBlank(message="Solicitação de recuperação inválida.") @Size(max=36,message="Solicitação de recuperação inválida.") String desafio,
        @NotNull(message="Informe o código.") @Pattern(regexp="[0-9]{6}",message="O código deve ter 6 dígitos.") String codigo,
        @NotBlank(message="Informe a nova senha.") @Size(min=8,max=128,message="A nova senha deve ter entre 8 e 128 caracteres.") String novaSenha){}
}
