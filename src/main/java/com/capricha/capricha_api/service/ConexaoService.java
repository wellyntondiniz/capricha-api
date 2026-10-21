package com.capricha.capricha_api.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.capricha.capricha_api.entidade.CodigoConexao;
import com.capricha.capricha_api.entidade.ConexaoParticipantes;
import com.capricha.capricha_api.entidade.ConexaoParticipantes.Status;
import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Participacao;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.repository.CodigoConexaoRepository;
import com.capricha.capricha_api.repository.ConexaoParticipantesRepository;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.ParticipacaoRepository;
import com.capricha.capricha_api.rest.CheckInException;


@Service
public class ConexaoService {
    private static final ZoneId FUSO = ZoneId.of("America/Cuiaba");

    private final ConexaoParticipantesRepository conexoes;
    private final CodigoConexaoRepository codigos;
    private final EventoRepository eventos;
    private final ParticipacaoRepository participacoes;
    private final CheckInQrService qr;
    private final int xpPorConexao;

    public ConexaoService(ConexaoParticipantesRepository conexoes, CodigoConexaoRepository codigos,
            EventoRepository eventos, ParticipacaoRepository participacoes, CheckInQrService qr,
            @Value("${app.conexao.xp:150}") int xpPorConexao) {
        this.conexoes = conexoes;
        this.codigos = codigos;
        this.eventos = eventos;
        this.participacoes = participacoes;
        this.qr = qr;
        this.xpPorConexao = xpPorConexao;
    }

    public record MeuQr(String payload, String imageData) {}
    public record Perfil(String nome, String foto, String relacao, Long solicitacaoId) {}
    public record Solicitacao(Long id, String nome, String foto, Integer eventoId, String eventoNome, Instant criadaEm) {}
    public record Conectado(Long id, String nome, String foto, Integer eventoId, String eventoNome,
            Instant conectadaEm, int xp) {}
    public record Resumo(List<Solicitacao> recebidas, List<Solicitacao> enviadas, List<Conectado> conexoes) {}
    public record Resultado(Long id, String status, int xp, String message) {}


    public MeuQr meuQr(Usuario usuario) {
        CodigoConexao codigo = codigos.findByUsuarioId(usuario.getId()).orElseGet(() -> criarCodigo(usuario));
        String payload = "{\"type\":\"USER_CONNECT\",\"token\":\"" + codigo.getToken() + "\"}";
        return new MeuQr(payload, qr.imagem(payload));
    }

    private CodigoConexao criarCodigo(Usuario usuario) {
        try {
            return codigos.saveAndFlush(new CodigoConexao(usuario, UUID.randomUUID().toString()));
        } catch (DataIntegrityViolationException e) {
            return codigos.findByUsuarioId(usuario.getId()).orElseThrow(() -> e);
        }
    }

    /** Mostra o perfil do dono do QR e a situação da dupla neste evento. Não altera nada. */
    @Transactional(readOnly = true)
    public Perfil identificar(Integer eventoId, String token, Usuario eu) {
        eventos.findById(eventoId).orElseThrow(this::eventoNaoEncontrado);
        Usuario alvo = participante(token, eu);
        String chave = chave(eventoId, eu.getId(), alvo.getId());
        if (conexoes.existsByChaveConfirmada(chave)) {
            return new Perfil(alvo.getNome(), alvo.getFoto(), "CONNECTED", null);
        }
        return conexoes.findByChavePendente(chave)
                .map(c -> new Perfil(alvo.getNome(), alvo.getFoto(),
                        c.getSolicitante().getId().equals(eu.getId()) ? "PENDING_SENT" : "PENDING_RECEIVED", c.getId()))
                .orElseGet(() -> new Perfil(alvo.getNome(), alvo.getFoto(), "NONE", null));
    }

    @Transactional
    public Solicitacao solicitar(Integer eventoId, String token, Usuario eu) {
        // Bloqueia o evento primeiro: criar, aceitar e recusar no mesmo evento passam um de cada vez.
        Evento evento = eventoEmAndamento(eventoId);
        Usuario alvo = participante(token, eu);
        exigirPresenca(evento, eu.getId(), "CHECK_IN_REQUIRED", "Faça check-in neste evento antes de se conectar.");
        exigirPresenca(evento, alvo.getId(), "PEER_NOT_CHECKED_IN", "Este participante ainda não fez check-in neste evento.");
        String chave = chave(evento.getId(), eu.getId(), alvo.getId());
        if (conexoes.existsByChaveConfirmada(chave)) throw jaConectados();
        if (conexoes.existsByChavePendente(chave)) throw pendente();
        try {
            ConexaoParticipantes c = conexoes.saveAndFlush(
                    new ConexaoParticipantes(evento, eu, alvo, Instant.now(), chave));
            return new Solicitacao(c.getId(), alvo.getNome(), alvo.getFoto(), evento.getId(), evento.getNome(), c.getCriadaEm());
        } catch (DataIntegrityViolationException e) {
            throw pendente();
        }
    }

    @Transactional(readOnly = true)
    public Resumo resumo(Usuario eu) {
        LocalDateTime agora = LocalDateTime.now(FUSO);
        // Pendentes de evento já encerrado não podem mais ser aceitas, então nem aparecem.
        List<Solicitacao> recebidas = conexoes.doDestinatario(eu.getId(), Status.PENDENTE).stream()
                .filter(c -> emAndamento(c.getEvento(), agora))
                .map(c -> solicitacao(c, c.getSolicitante())).toList();
        List<Solicitacao> enviadas = conexoes.doSolicitante(eu.getId(), Status.PENDENTE).stream()
                .filter(c -> emAndamento(c.getEvento(), agora))
                .map(c -> solicitacao(c, c.getDestinatario())).toList();
        List<Conectado> confirmadas = conexoes.doUsuario(eu.getId(), Status.ACEITA).stream().map(c -> {
            Usuario outro = c.getSolicitante().getId().equals(eu.getId()) ? c.getDestinatario() : c.getSolicitante();
            return new Conectado(c.getId(), outro.getNome(), outro.getFoto(), c.getEvento().getId(),
                    c.getEvento().getNome(), c.getRespondidaEm(), c.getXpConcedido() == null ? 0 : c.getXpConcedido());
        }).toList();
        return new Resumo(recebidas, enviadas, confirmadas);
    }

    /**
     * Confirma a conexão e concede XP aos dois na MESMA transação: se qualquer passo falhar,
     * nada é gravado (nem a conexão, nem o XP de um dos lados).
     */
    @Transactional
    public Resultado aceitar(Long id, Usuario eu) {
        ConexaoParticipantes c = minhaSolicitacaoBloqueada(id, eu);
        Evento evento = eventoEmAndamento(c.getEvento().getId());
        if (c.getStatus() != Status.PENDENTE) throw respondida();
        Integer outroId = c.getSolicitante().getId();
        Participacao minha = exigirPresenca(evento, eu.getId(), "CHECK_IN_REQUIRED", "Faça check-in neste evento antes de se conectar.");
        Participacao dele = exigirPresenca(evento, outroId, "PEER_NOT_CHECKED_IN", "O outro participante não tem check-in neste evento.");
        String chave = c.getChavePendente();
        if (conexoes.existsByChaveConfirmada(chave)) throw jaConectados();
        c.aceitar(Instant.now(), xpPorConexao);
        try {
            conexoes.saveAndFlush(c);
        } catch (DataIntegrityViolationException e) {
            throw jaConectados();
        }
        somarXp(minha);
        somarXp(dele);
        return new Resultado(c.getId(), "ACEITA", xpPorConexao, "Conexão confirmada! Vocês ganharam " + xpPorConexao + " XP.");
    }

    @Transactional
    public Resultado recusar(Long id, Usuario eu) {
        ConexaoParticipantes c = minhaSolicitacaoBloqueada(id, eu);
        if (c.getStatus() != Status.PENDENTE) throw respondida();
        c.recusar(Instant.now());
        conexoes.saveAndFlush(c);
        return new Resultado(c.getId(), "RECUSADA", 0, "Solicitação recusada.");
    }

    private void somarXp(Participacao p) {
        if (conexoes.somarScore(p.getId(), xpPorConexao) != 1) {
            throw new IllegalStateException("Não foi possível atualizar a pontuação; a conexão foi desfeita.");
        }
    }

    /** Só o destinatário enxerga e responde; para qualquer outro a solicitação "não existe". */
    private ConexaoParticipantes minhaSolicitacaoBloqueada(Long id, Usuario eu) {
        return conexoes.buscarParaResposta(id)
                .filter(c -> c.getDestinatario().getId().equals(eu.getId()))
                .orElseThrow(() -> new CheckInException(404, "REQUEST_NOT_FOUND", "Solicitação não encontrada."));
    }

    private Usuario participante(String token, Usuario eu) {
        Usuario alvo = codigos.findByToken(token).map(CodigoConexao::getUsuario).filter(Usuario::getAtivo)
                .orElseThrow(() -> new CheckInException(404, "USER_NOT_FOUND",
                        "Este QR Code não pertence a um participante existente."));
        if (alvo.getId().equals(eu.getId())) {
            throw new CheckInException(409, "SELF_CONNECTION", "Você não pode se conectar consigo mesmo.");
        }
        return alvo;
    }

    private Evento eventoEmAndamento(Integer eventoId) {
        Evento evento = eventos.buscarParaCheckIn(eventoId).orElseThrow(this::eventoNaoEncontrado);
        if (!emAndamento(evento, LocalDateTime.now(FUSO))) {
            throw new CheckInException(409, "CONNECTION_NOT_AVAILABLE", "Conexões só estão disponíveis enquanto o evento está em andamento.");
        }
        return evento;
    }

    private boolean emAndamento(Evento e, LocalDateTime agora) {
        return CheckInDisponibilidade.disponivel(e.getAtivo(), e.getDataInicio(), e.getDataTermino(), agora);
    }

    private Participacao exigirPresenca(Evento evento, Integer usuarioId, String codigo, String mensagem) {
        return participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuarioId, evento.getId()).stream()
                .filter(p -> p.getCheckedInAt() != null).findFirst()
                .orElseThrow(() -> new CheckInException(409, codigo, mensagem));
    }

    private Solicitacao solicitacao(ConexaoParticipantes c, Usuario outro) {
        return new Solicitacao(c.getId(), outro.getNome(), outro.getFoto(), c.getEvento().getId(),
                c.getEvento().getNome(), c.getCriadaEm());
    }

    /** Mesma chave para A→B e B→A. */
    private static String chave(Integer eventoId, Integer a, Integer b) {
        return eventoId + ":" + Math.min(a, b) + ":" + Math.max(a, b);
    }

    private CheckInException eventoNaoEncontrado() {
        return new CheckInException(404, "EVENT_NOT_FOUND", "Evento não encontrado.");
    }

    private CheckInException jaConectados() {
        return new CheckInException(409, "ALREADY_CONNECTED", "Vocês já estão conectados neste evento.");
    }

    private CheckInException pendente() {
        return new CheckInException(409, "CONNECTION_PENDING", "Já existe uma solicitação pendente entre vocês neste evento.");
    }

    private CheckInException respondida() {
        return new CheckInException(409, "REQUEST_NOT_PENDING", "Esta solicitação já foi respondida.");
    }
}