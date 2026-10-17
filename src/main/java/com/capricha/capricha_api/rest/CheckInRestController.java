package com.capricha.capricha_api.rest;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.capricha.capricha_api.service.CheckInService;
import com.capricha.capricha_api.service.SessaoUsuarioService;

@RestController
@RequestMapping("/evento")
@CrossOrigin
public class CheckInRestController {
    private final CheckInService checkIn;
    private final SessaoUsuarioService sessoes;

    public CheckInRestController(CheckInService checkIn, SessaoUsuarioService sessoes) {
        this.checkIn = checkIn;
        this.sessoes = sessoes;
    }

    public record Pedido(@NotBlank @Size(max = 36) String qrToken) {}

    @GetMapping("/check-in")
    public List<CheckInService.EventoCheckIn> listar(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.listar(sessoes.autenticar(authorization));
    }

    @PostMapping("/{id}/check-in/validar")
    public CheckInService.EventoCheckIn identificar(@PathVariable Integer id, @Valid @RequestBody Pedido pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.identificar(id, pedido.qrToken(), sessoes.autenticar(authorization));
    }

    @PostMapping("/{id}/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInService.Registro registrar(@PathVariable Integer id, @Valid @RequestBody Pedido pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.registrar(id, pedido.qrToken(), sessoes.autenticar(authorization));
    }

    @GetMapping("/{id}/qr-code")
    public CheckInService.QrCode qr(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.gerarQr(id, sessoes.autenticar(authorization));
    }
}
