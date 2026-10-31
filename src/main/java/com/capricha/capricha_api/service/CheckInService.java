package com.capricha.capricha_api.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.capricha.capricha_api.entidade.*;
import com.capricha.capricha_api.repository.*;
import com.capricha.capricha_api.rest.CheckInException;
import com.capricha.capricha_api.model.Palestra;

@Service
public class CheckInService {
    // Mesmo fuso usado pelo cadastro de eventos da develop.
    private static final ZoneId FUSO = ZoneId.of("America/Cuiaba");
    public static final int PONTOS_CHECK_IN_EVENTO = 150;
    public static final int PONTOS_CHECK_IN_PALESTRA = 150;
    private final EventoRepository eventos;
    private final ParticipacaoRepository participacoes;
    private final CheckInQrService qr;
    private final PalestraRepository palestras;
    private final PresencaPalestraRepository presencasPalestra;
    @Autowired
    private ParticipacaoService participacaoService;

    public CheckInService(EventoRepository eventos, ParticipacaoRepository participacoes, CheckInQrService qr,
            PalestraRepository palestras, PresencaPalestraRepository presencasPalestra) {
        this.eventos = eventos;
        this.participacoes = participacoes;
        this.qr = qr;
        this.palestras = palestras;
        this.presencasPalestra = presencasPalestra;
    }

    public record EventoCheckIn(Integer id, String nome, String descricao, OffsetDateTime dataInicio,
            OffsetDateTime dataTermino, String status, boolean canCheckIn, boolean hasCheckedIn, Instant checkedInAt) {}
    public record Registro(Integer id, Integer eventId, String eventName, Instant checkedInAt, String message) {}
    public record QrCode(Integer eventId, String eventName, String payload, String imageData) {}
    public record QrCodePalestra(Long lectureId, String lectureName, String payload, String imageData) {}
    public record PalestraCheckIn(Long id, String nome, String descricao, OffsetDateTime dataInicio,
            OffsetDateTime dataTermino, String status, boolean canCheckIn, boolean hasCheckedIn,
            Instant checkedInAt, Integer eventoId, String palestrante) {}
    public record RegistroPalestra(Long id, Long lectureId, String lectureName, Instant checkedInAt, String message) {}

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
        // Os pontos da presença valem igualmente para quem já tinha inscrição (antes só quem não tinha recebia).
        participacao.setScore((participacao.getScore() == null ? 0 : participacao.getScore()) + PONTOS_CHECK_IN_EVENTO);
        participacao.setCheckedInAt(Instant.now());
        participacoes.saveAndFlush(participacao);
        return new Registro(participacao.getId(), id, evento.getNome(), participacao.getCheckedInAt(),
                "Check-in realizado com sucesso!");
    }

    @Transactional
    public QrCode gerarQr(Integer id, Usuario usuario) {
        qr.exigirAdministrador(usuario);
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
        return new QrCode(id, evento.getNome(), payload, qr.imagem(payload));
    }

    private EventoCheckIn resposta(Evento e, Instant presenca, LocalDateTime agora) {
        String status = !e.getAtivo() || (e.getDataTermino() != null && !agora.isBefore(e.getDataTermino()))
                ? "finished" : e.getDataInicio() == null || agora.isBefore(e.getDataInicio()) ? "upcoming" : "available";
        return new EventoCheckIn(e.getId(), e.getNome(), e.getDescricao(), comFuso(e.getDataInicio()),
                comFuso(e.getDataTermino()), status, disponivel(e, agora), presenca != null, presenca);
    }

    private boolean disponivel(Evento e, LocalDateTime agora) {
        return CheckInDisponibilidade.disponivel(e.getAtivo(), e.getDataInicio(), e.getDataTermino(), agora);
    }

    private OffsetDateTime comFuso(LocalDateTime data) {
        return data == null ? null : data.atZone(FUSO).toOffsetDateTime();
    }

    private void validarToken(Evento evento, String token) {
        qr.validarToken(evento.getCheckInToken(), token, "INVALID_EVENT_TOKEN", "QR Code inválido para este evento.");
    }

    private CheckInException naoEncontrado() {
        return new CheckInException(404, "EVENT_NOT_FOUND", "Evento não encontrado.");
    }

    @Transactional
    public QrCodePalestra gerarQrPalestra(Long id, Usuario usuario) {
        qr.exigirAdministrador(usuario);
        Palestra palestra = palestras.buscarParaCheckIn(id).orElseThrow(() ->
                new CheckInException(404, "LECTURE_NOT_FOUND", "Palestra não encontrada."));
        if (palestra.getEvento() == null || !palestra.getEvento().getAtivo()) {
            throw new CheckInException(409, "CHECK_IN_NOT_AVAILABLE", "A palestra não possui um evento ativo.");
        }
        if (palestra.getCheckInToken() == null) {
            palestra.setCheckInToken(UUID.randomUUID().toString());
            palestras.save(palestra);
        }
        String payload = "{\"type\":\"LECTURE_CHECKIN\",\"lectureId\":" + palestra.getId()
                + ",\"token\":\"" + palestra.getCheckInToken() + "\"}";
        return new QrCodePalestra(id, palestra.getNome(), payload, qr.imagem(payload));
    }

    @Transactional(readOnly = true)
    public List<PalestraCheckIn> listarPalestras(Usuario usuario) {
        Map<Long, Instant> presencas = new HashMap<>();
        presencasPalestra.findByUsuarioId(usuario.getId()).forEach(p -> presencas.put(p.getPalestra().getId(), p.getCheckedInAt()));
        LocalDateTime agora = LocalDateTime.now(FUSO);
        return palestras.findByEventoAtivoTrue().stream()
                .map(p -> resposta(p, presencas.get(p.getId()), agora)).toList();
    }

    @Transactional(readOnly = true)
    public PalestraCheckIn identificarPalestra(Long id, String token, Usuario usuario) {
        Palestra palestra = palestras.findById(id).orElseThrow(this::palestraNaoEncontrada);
        validarToken(palestra, token);
        Instant presenca = presencasPalestra.findByUsuarioIdAndPalestraId(usuario.getId(), id)
                .map(PresencaPalestra::getCheckedInAt).orElse(null);
        return resposta(palestra, presenca, LocalDateTime.now(FUSO));
    }

    @Transactional
    public RegistroPalestra registrarPalestra(Long id, String token, Usuario usuario) {
        Palestra palestra = palestras.buscarParaCheckIn(id).orElseThrow(this::palestraNaoEncontrada);
        validarToken(palestra, token);
        if (presencasPalestra.findByUsuarioIdAndPalestraId(usuario.getId(), id).isPresent()) {
            throw presencaPalestraDuplicada();
        }
        LocalDateTime inicio = inicioPalestra(palestra);
        if (!CheckInDisponibilidade.disponivel(palestraAtiva(palestra), inicio,
                palestra.getDataTermino(), LocalDateTime.now(FUSO))) {
            throw new CheckInException(409, "CHECK_IN_NOT_AVAILABLE", "Check-in não disponível para esta palestra neste momento.");
        }
        // A disponibilidade acima já garante que a palestra tem um evento ativo.
        Integer eventoId = palestra.getEvento().getId();
        boolean presenteNoEvento = participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), eventoId).stream()
                .anyMatch(p -> p.getCheckedInAt() != null);
        if (!presenteNoEvento) {
            throw new CheckInException(409, "EVENT_CHECK_IN_REQUIRED",
                    "Faça o check-in no evento antes de registrar presença na palestra.");
        }
        PresencaPalestra presenca;
        try {
            presenca = presencasPalestra.saveAndFlush(new PresencaPalestra(usuario, palestra, Instant.now()));
            participacaoService.atualizarScore(usuario.getId(), eventoId, PONTOS_CHECK_IN_PALESTRA);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            String detalhe = e.getMostSpecificCause().getMessage();
            if (detalhe != null && detalhe.toLowerCase(Locale.ROOT).contains("uk_presenca_usuario_palestra")) {
                throw presencaPalestraDuplicada();
            }
            throw e;
        }
        return new RegistroPalestra(presenca.getId(), id, palestra.getNome(), presenca.getCheckedInAt(),
                "Check-in realizado com sucesso!");
    }

    private PalestraCheckIn resposta(Palestra p, Instant presenca, LocalDateTime agora) {
        LocalDateTime inicio = inicioPalestra(p);
        LocalDateTime termino = p.getDataTermino();
        boolean ativa = palestraAtiva(p);
        String status = !ativa || (termino != null && !agora.isBefore(termino))
                ? "finished" : inicio == null || agora.isBefore(inicio) ? "upcoming" : "available";
        return new PalestraCheckIn(p.getId(), p.getNome(), p.getDescricao(), comFuso(inicio), comFuso(termino),
                status, CheckInDisponibilidade.disponivel(ativa, inicio, termino, agora), presenca != null,
                presenca, p.getEvento() == null ? null : p.getEvento().getId(), p.getPalestrante());
    }

    private LocalDateTime inicioPalestra(Palestra p) {
        if (p.getData() == null || p.getHorario() == null) return null;
        try {
            var formato = java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT);
            return LocalDateTime.parse(p.getData().trim() + " " + p.getHorario().trim(), formato);
        } catch (java.time.format.DateTimeParseException e) {
            return null; // Cadastro antigo inválido não derruba a lista nem libera check-in.
        }
    }

    private boolean palestraAtiva(Palestra p) {
        // Palestra não tem flag de estado própria; usa a situação do evento associado.
        return p.getEvento() != null && p.getEvento().getAtivo();
    }

    private void validarToken(Palestra palestra, String token) {
        qr.validarToken(palestra.getCheckInToken(), token, "INVALID_LECTURE_TOKEN", "QR Code inválido para esta palestra.");
    }

    private CheckInException palestraNaoEncontrada() {
        return new CheckInException(404, "LECTURE_NOT_FOUND", "Palestra não encontrada.");
    }

    private CheckInException presencaPalestraDuplicada() {
        return new CheckInException(409, "ALREADY_CHECKED_IN", "Você já realizou check-in nesta palestra.");
    }

}
