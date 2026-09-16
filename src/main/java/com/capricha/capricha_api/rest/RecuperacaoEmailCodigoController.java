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
    public record Solicitar(@NotBlank @Email @Size(max=254) String email){}
    public record Codigo(@NotBlank @Size(max=36) String desafio,@Pattern(regexp="[0-9]{6}") String codigo){}
    public record Redefinir(@NotBlank @Size(max=36) String desafio,@Pattern(regexp="[0-9]{6}") String codigo,
        @NotBlank @Size(min=8,max=128) String novaSenha){}
}
