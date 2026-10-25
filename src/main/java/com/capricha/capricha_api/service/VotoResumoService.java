package com.capricha.capricha_api.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Participacao;
import com.capricha.capricha_api.entidade.Resumo;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.entidade.VotoResumo;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.ParticipacaoRepository;
import com.capricha.capricha_api.repository.ResumoRepository;
import com.capricha.capricha_api.repository.VotoResumoRepository;
import com.capricha.capricha_api.rest.CheckInException;


@Service
public class VotoResumoService {

	public static final int LIMITE_VOTOS_POR_EVENTO = 3;

	public record Situacao(Integer eventoId, boolean hasCheckedIn, int votesUsed, int votesLimit, int votesRemaining,
			List<Integer> votedResumoIds, boolean canVote) {
	}

	public record Registro(Long id, Integer resumoId, String resumoTitulo, Integer eventoId, Instant votedAt,
			int votesUsed, int votesRemaining, String message) {
	}

	public record QrCodeResumo(Integer resumoId, String resumoTitulo, String payload, String imageData) {
	}

	private final ResumoRepository resumos;
	private final EventoRepository eventos;
	private final ParticipacaoRepository participacoes;
	private final VotoResumoRepository votos;
	private final CheckInQrService qr;

	public VotoResumoService(ResumoRepository resumos, EventoRepository eventos, ParticipacaoRepository participacoes,
			VotoResumoRepository votos, CheckInQrService qr) {
		this.resumos = resumos;
		this.eventos = eventos;
		this.participacoes = participacoes;
		this.votos = votos;
		this.qr = qr;
	}

	@Transactional(readOnly = true)
	public Situacao situacao(Integer eventoId, Usuario usuario) {
		Evento evento = eventos.findById(eventoId)
				.orElseThrow(() -> new CheckInException(404, "EVENT_NOT_FOUND", "Evento não encontrado."));
		boolean checkIn = fezCheckIn(usuario, evento.getId());
		List<Integer> votados = votos.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), evento.getId()).stream()
				.map(v -> v.getResumo().getId()).toList();
		int restantes = Math.max(0, LIMITE_VOTOS_POR_EVENTO - votados.size());
		return new Situacao(evento.getId(), checkIn, votados.size(), LIMITE_VOTOS_POR_EVENTO, restantes, votados,
				evento.getAtivo() && checkIn && restantes > 0);
	}

	@Transactional
	public Registro votar(Integer resumoId, Usuario usuario) {
		Resumo resumo = resumos.findById(resumoId).orElseThrow(this::resumoNaoEncontrado);
		// O bloqueio da linha do evento serializa votos concorrentes do mesmo evento, inclusive entre instâncias.
		// É ele que garante o limite de votos por evento; o banco garante sozinho só o "1 por resumo".
		Evento evento = eventos.buscarParaCheckIn(resumo.getEvento().getId())
				.orElseThrow(() -> new CheckInException(404, "EVENT_NOT_FOUND", "Evento não encontrado."));
		if (!evento.getAtivo()) {
			throw new CheckInException(409, "EVENT_NOT_ACTIVE", "Este evento está desativado e não aceita votos.");
		}
		if (!fezCheckIn(usuario, evento.getId())) {
			throw new CheckInException(403, "NOT_CHECKED_IN",
					"Somente quem fez check-in no evento pode votar. Faça o check-in e tente novamente.");
		}
		if (votos.existsByUsuarioIdAndResumoId(usuario.getId(), resumo.getId())) {
			throw votoDuplicado();
		}
		if (votos.countByUsuarioIdAndEventoId(usuario.getId(), evento.getId()) >= LIMITE_VOTOS_POR_EVENTO) {
			throw new CheckInException(409, "VOTE_LIMIT_REACHED", "Você já usou os " + LIMITE_VOTOS_POR_EVENTO
					+ " votos deste evento.");
		}
		VotoResumo voto;
		try {
			voto = votos.saveAndFlush(new VotoResumo(usuario, resumo, evento, Instant.now()));
		} catch (DataIntegrityViolationException e) {
			String detalhe = e.getMostSpecificCause().getMessage();
			if (detalhe != null && detalhe.toLowerCase(Locale.ROOT).contains("uk_voto_usuario_resumo")) {
				throw votoDuplicado();
			}
			throw e;
		}
		int usados = (int) votos.countByUsuarioIdAndEventoId(usuario.getId(), evento.getId());
		return new Registro(voto.getId(), resumo.getId(), resumo.getTitulo(), evento.getId(), voto.getVotadoEm(),
				usados, Math.max(0, LIMITE_VOTOS_POR_EVENTO - usados), "Voto registrado com sucesso!");
	}

	@Transactional(readOnly = true)
	public QrCodeResumo gerarQr(Integer resumoId, Usuario usuario) {
		qr.exigirAdministrador(usuario);
		Resumo resumo = resumos.findById(resumoId).orElseThrow(this::resumoNaoEncontrado);
		if (!resumo.getEvento().getAtivo()) {
			throw new CheckInException(409, "EVENT_NOT_ACTIVE", "O resumo pertence a um evento desativado.");
		}
		// O QR só aponta para o resumo. Quem pode votar é decidido pela sessão e pelo check-in, nunca pelo QR.
		String payload = "{\"type\":\"RESUMO_VOTE\",\"resumoId\":" + resumo.getId() + "}";
		return new QrCodeResumo(resumo.getId(), resumo.getTitulo(), payload, qr.imagem(payload));
	}

	private boolean fezCheckIn(Usuario usuario, Integer eventoId) {
		return participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), eventoId).stream()
				.map(Participacao::getCheckedInAt).anyMatch(Objects::nonNull);
	}

	private CheckInException resumoNaoEncontrado() {
		return new CheckInException(404, "RESUMO_NOT_FOUND", "Resumo não encontrado.");
	}

	private CheckInException votoDuplicado() {
		return new CheckInException(409, "ALREADY_VOTED", "Você já votou neste resumo. Cada resumo recebe só 1 voto seu.");
	}
}