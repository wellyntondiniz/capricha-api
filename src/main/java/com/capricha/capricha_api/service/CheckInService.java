package com.capricha.capricha_api.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.capricha.capricha_api.entidade.*;
import com.capricha.capricha_api.repository.*;
import com.capricha.capricha_api.rest.CheckInException;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;

@Service
public class CheckInService {
    // Mesmo fuso usado pelo cadastro de eventos da develop.
    private static final ZoneId FUSO = ZoneId.of("America/Cuiaba");
    private final EventoRepository eventos;
    private final ParticipacaoRepository participacoes;

    public CheckInService(EventoRepository eventos, ParticipacaoRepository participacoes) {
        this.eventos = eventos;
        this.participacoes = participacoes;
    }

    public record EventoCheckIn(Integer id, String nome, String descricao, OffsetDateTime dataInicio,
            OffsetDateTime dataTermino, String status, boolean canCheckIn, boolean hasCheckedIn, Instant checkedInAt) {}
    public record Registro(Integer id, Integer eventId, String eventName, Instant checkedInAt, String message) {}
    public record QrCode(Integer eventId, String eventName, String payload, String imageData) {}

    @Transactional(readOnly = true)
    public List<EventoCheckIn> listar(Usuario usuario) {
        Map<Integer, Instant> presencas = new HashMap<>();
        participacoes.findByUsuarioIdAndCheckedInAtIsNotNull(usuario.getId()).forEach(p ->
                presencas.putIfAbsent(p.getEvento().getId(), p.getCheckedInAt()));
        LocalDateTime agora = LocalDateTime.now(FUSO);
        return eventos.findAllByAtivo(true).stream()
                .map(e -> resposta(e, presencas.get(e.getId()), agora)).toList();
    }

    @Transactional(readOnly = true)
    public EventoCheckIn identificar(Integer id, String token, Usuario usuario) {
        Evento evento = eventos.findById(id).orElseThrow(this::naoEncontrado);
        validarToken(evento, token);
        Instant presenca = participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), id).stream()
                .map(Participacao::getCheckedInAt).filter(Objects::nonNull).findFirst().orElse(null);
        return resposta(evento, presenca, LocalDateTime.now(FUSO));
    }

    @Transactional
    public Registro registrar(Integer id, String token, Usuario usuario) {
        // O bloqueio da linha do evento serializa confirmações inclusive entre instâncias da API.
        // Não exige apagar participações duplicadas que possam existir no banco antigo.
        Evento evento = eventos.buscarParaCheckIn(id).orElseThrow(this::naoEncontrado);
        validarToken(evento, token);
        List<Participacao> existentes = participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), id);
        if (existentes.stream().anyMatch(p -> p.getCheckedInAt() != null)) {
            throw new CheckInException(409, "ALREADY_CHECKED_IN", "Você já realizou check-in neste evento.");
        }
        if (!disponivel(evento, LocalDateTime.now(FUSO))) {
            throw new CheckInException(409, "CHECK_IN_NOT_AVAILABLE", "Check-in não disponível neste momento.");
        }
        Participacao participacao;
        if (existentes.isEmpty()) {
            participacao = new Participacao();
            participacao.setEvento(evento);
            participacao.setUsuario(usuario);
            participacao.setScore(0);
        } else {
            participacao = existentes.get(0);
        }
        participacao.setCheckedInAt(Instant.now());
        participacoes.saveAndFlush(participacao);
        return new Registro(participacao.getId(), id, evento.getNome(), participacao.getCheckedInAt(),
                "Check-in realizado com sucesso!");
    }

    @Transactional
    public QrCode gerarQr(Integer id, Usuario usuario) {
        String perfil = usuario.getPerfil();
        if (!"ADMIN".equalsIgnoreCase(perfil) && !"ADMINISTRADOR".equalsIgnoreCase(perfil)) {
            throw new CheckInException(403, "FORBIDDEN", "Somente administradores podem gerar o QR Code.");
        }
        Evento evento = eventos.buscarParaCheckIn(id).orElseThrow(this::naoEncontrado);
        if (!evento.getAtivo()) {
            throw new CheckInException(409, "CHECK_IN_NOT_AVAILABLE", "Evento desativado.");
        }
        if (evento.getCheckInToken() == null) {
            evento.setCheckInToken(UUID.randomUUID().toString());
            eventos.save(evento);
        }
        String payload = "{\"type\":\"EVENT_CHECKIN\",\"eventId\":" + evento.getId()
                + ",\"token\":\"" + evento.getCheckInToken() + "\"}";
        return new QrCode(id, evento.getNome(), payload, imagemQr(payload));
    }

    private EventoCheckIn resposta(Evento e, Instant presenca, LocalDateTime agora) {
        String status = !e.getAtivo() || (e.getDataTermino() != null && !agora.isBefore(e.getDataTermino()))
                ? "finished" : e.getDataInicio() == null || agora.isBefore(e.getDataInicio()) ? "upcoming" : "available";
        return new EventoCheckIn(e.getId(), e.getNome(), e.getDescricao(), comFuso(e.getDataInicio()),
                comFuso(e.getDataTermino()), status, disponivel(e, agora), presenca != null, presenca);
    }

    private boolean disponivel(Evento e, LocalDateTime agora) {
        return e.getAtivo() && e.getDataInicio() != null && !agora.isBefore(e.getDataInicio())
                && (e.getDataTermino() == null || agora.isBefore(e.getDataTermino()));
    }

    private OffsetDateTime comFuso(LocalDateTime data) {
        return data == null ? null : data.atZone(FUSO).toOffsetDateTime();
    }

    private void validarToken(Evento evento, String token) {
        if (evento.getCheckInToken() == null || token == null || !MessageDigest.isEqual(
                evento.getCheckInToken().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new CheckInException(400, "INVALID_EVENT_TOKEN", "QR Code inválido para este evento.");
        }
    }

    private CheckInException naoEncontrado() {
        return new CheckInException(404, "EVENT_NOT_FOUND", "Evento não encontrado.");
    }

    private String imagemQr(String payload) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 360, 360,
                    Map.of(EncodeHintType.MARGIN, 1));
            BufferedImage imagem = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 360; x++) {
                for (int y = 0; y < 360; y++) imagem.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(imagem, "PNG", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new CheckInException(500, "QR_GENERATION_ERROR", "Não foi possível gerar o QR Code.");
        }
    }
}
