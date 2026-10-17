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
@CrossOrigin
public class CheckInRestController {
    private final CheckInService checkIn;
    private final SessaoUsuarioService sessoes;

    public CheckInRestController(CheckInService checkIn, SessaoUsuarioService sessoes) {
        this.checkIn = checkIn;
        this.sessoes = sessoes;
    }

    public record Pedido(@NotBlank @Size(max = 36) String qrToken) {}
    public record PedidoPalestra(@NotBlank String type,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long lectureId,
            @NotBlank @Size(max = 36) String qrToken) {}

    @GetMapping("/evento/check-in")
    public List<CheckInService.EventoCheckIn> listar(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.listar(sessoes.autenticar(authorization));
    }

    @PostMapping("/evento/{id}/check-in/validar")
    public CheckInService.EventoCheckIn identificar(@PathVariable Integer id, @Valid @RequestBody Pedido pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.identificar(id, pedido.qrToken(), sessoes.autenticar(authorization));
    }

    @PostMapping("/evento/{id}/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInService.Registro registrar(@PathVariable Integer id, @Valid @RequestBody Pedido pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.registrar(id, pedido.qrToken(), sessoes.autenticar(authorization));
    }

    @GetMapping("/evento/{id}/qr-code")
    public CheckInService.QrCode qr(@PathVariable Integer id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.gerarQr(id, sessoes.autenticar(authorization));
    }

    @GetMapping("/palestras/{id}/qr-code")
    public CheckInService.QrCodePalestra qrPalestra(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.gerarQrPalestra(id, sessoes.autenticar(authorization));
    }

    @GetMapping("/palestras/check-in")
    public List<CheckInService.PalestraCheckIn> listarPalestras(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return checkIn.listarPalestras(sessoes.autenticar(authorization));
    }

    @PostMapping("/palestras/{id}/check-in/validar")
    public CheckInService.PalestraCheckIn identificarPalestra(@PathVariable Long id,
            @Valid @RequestBody PedidoPalestra pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        var usuario = sessoes.autenticar(authorization);
        validarPedidoPalestra(id, pedido);
        return checkIn.identificarPalestra(id, pedido.qrToken(), usuario);
    }

    @PostMapping("/palestras/{id}/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckInService.RegistroPalestra registrarPalestra(@PathVariable Long id,
            @Valid @RequestBody PedidoPalestra pedido,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        var usuario = sessoes.autenticar(authorization);
        validarPedidoPalestra(id, pedido);
        return checkIn.registrarPalestra(id, pedido.qrToken(), usuario);
    }

    private void validarPedidoPalestra(Long id, PedidoPalestra pedido) {
        if (!"LECTURE_CHECKIN".equals(pedido.type()) || !id.equals(pedido.lectureId())) {
            throw new CheckInException(400, "INVALID_QR", "O tipo ou o ID do QR Code não corresponde à palestra selecionada.");
        }
    }
}
