package com.capricha.capricha_api.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.capricha.capricha_api.entidade.*;
import com.capricha.capricha_api.repository.*;
import com.capricha.capricha_api.service.Senhas;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class VotoResumoIntegracaoTest {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired EventoRepository eventos;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired ResumoRepository resumos;
    @Autowired VotoResumoRepository votos;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private Usuario usuario;
    private Usuario admin;
    private Evento eventoA;
    private Evento eventoB;
    private Resumo resumoA1;
    private Resumo resumoA2;
    private Resumo resumoA3;
    private Resumo resumoA4;
    private Resumo resumoB1;
    private String token;
    private String adminToken;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        limpar();
        usuario = usuario("votante@teste.com", "PARTICIPANTE");
        admin = usuario("admin-voto@teste.com", "ADMIN");
        eventoA = evento("Evento A");
        eventoB = evento("Evento B");
        resumoA1 = resumo("Resumo A1", eventoA);
        resumoA2 = resumo("Resumo A2", eventoA);
        resumoA3 = resumo("Resumo A3", eventoA);
        resumoA4 = resumo("Resumo A4", eventoA);
        resumoB1 = resumo("Resumo B1", eventoB);
        token = login(usuario);
        adminToken = login(admin);
    }

    @AfterEach
    void limpar() {
        votos.deleteAll();
        resumos.deleteAll();
        sessoes.deleteAll();
        participacoes.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
    }

    private Usuario usuario(String email, String perfil) {
        Usuario u = new Usuario();
        u.setNome(perfil);
        u.setEmail(email);
        u.setSenha(Senhas.proteger("Senha@123"));
        u.setPerfil(perfil);
        return usuarios.saveAndFlush(u);
    }

    private Evento evento(String nome) {
        Evento e = new Evento();
        e.setNome(nome);
        return eventos.saveAndFlush(e);
    }

    private Resumo resumo(String titulo, Evento evento) {
        Resumo r = new Resumo();
        r.setTitulo(titulo);
        r.setTexto("Texto do " + titulo);
        r.setAutores(new ArrayList<>(List.of("Autora Teste")));
        r.setEvento(evento);
        return resumos.saveAndFlush(r);
    }

    private void checkIn(Usuario u, Evento e) {
        Participacao p = new Participacao();
        p.setUsuario(u);
        p.setEvento(e);
        p.setScore(0);
        p.setCheckedInAt(Instant.now());
        participacoes.saveAndFlush(p);
    }

    private String login(Usuario u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "Senha@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asString();
    }

    private org.springframework.test.web.servlet.ResultActions votar(Resumo r, String credencial) throws Exception {
        return mvc.perform(post("/resumos/" + r.getId() + "/voto").header("Authorization", "Bearer " + credencial));
    }

    @Test
    void participanteComCheckInVotaEASituacaoRefleteOsVotos() throws Exception {
        checkIn(usuario, eventoA);
        mvc.perform(get("/evento/" + eventoA.getId() + "/votacao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasCheckedIn").value(true))
                .andExpect(jsonPath("$.votesUsed").value(0)).andExpect(jsonPath("$.votesLimit").value(3))
                .andExpect(jsonPath("$.votesRemaining").value(3)).andExpect(jsonPath("$.canVote").value(true));

        votar(resumoA1, token).andExpect(status().isCreated())
                .andExpect(jsonPath("$.resumoId").value(resumoA1.getId()))
                .andExpect(jsonPath("$.eventoId").value(eventoA.getId()))
                .andExpect(jsonPath("$.votesUsed").value(1)).andExpect(jsonPath("$.votesRemaining").value(2))
                .andExpect(jsonPath("$.votedAt").isString());

        assertEquals(1, votos.count());
        mvc.perform(get("/evento/" + eventoA.getId() + "/votacao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.votesUsed").value(1))
                .andExpect(jsonPath("$.votesRemaining").value(2))
                .andExpect(jsonPath("$.votedResumoIds[0]").value(resumoA1.getId()))
                .andExpect(jsonPath("$.canVote").value(true));
    }

    @Test
    void naoVotaDuasVezesNoMesmoResumo() throws Exception {
        checkIn(usuario, eventoA);
        votar(resumoA1, token).andExpect(status().isCreated());
        votar(resumoA1, token).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_VOTED"));
        assertEquals(1, votos.count());
        // O voto repetido não consome a cota: ainda dá para votar em outros dois resumos.
        votar(resumoA2, token).andExpect(status().isCreated());
        votar(resumoA3, token).andExpect(status().isCreated());
        assertEquals(3, votos.count());
    }

    @Test
    void limiteEDeTresVotosPorEvento() throws Exception {
        checkIn(usuario, eventoA);
        votar(resumoA1, token).andExpect(status().isCreated());
        votar(resumoA2, token).andExpect(status().isCreated());
        votar(resumoA3, token).andExpect(status().isCreated()).andExpect(jsonPath("$.votesRemaining").value(0));
        votar(resumoA4, token).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VOTE_LIMIT_REACHED"));
        assertEquals(3, votos.count());
        mvc.perform(get("/evento/" + eventoA.getId() + "/votacao").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.votesUsed").value(3)).andExpect(jsonPath("$.votesRemaining").value(0))
                .andExpect(jsonPath("$.canVote").value(false));
    }

    @Test
    void ocontadorEPorEventoEPorUsuario() throws Exception {
        checkIn(usuario, eventoA);
        checkIn(usuario, eventoB);
        votar(resumoA1, token).andExpect(status().isCreated());
        votar(resumoA2, token).andExpect(status().isCreated());
        votar(resumoA3, token).andExpect(status().isCreated());
        // Os 3 votos do evento A não afetam o evento B.
        votar(resumoB1, token).andExpect(status().isCreated());
        // Outro participante tem a própria cota no mesmo resumo.
        Usuario outro = usuario("outro-votante@teste.com", "PARTICIPANTE");
        checkIn(outro, eventoA);
        votar(resumoA1, login(outro)).andExpect(status().isCreated());
        assertEquals(5, votos.count());
    }

    @Test
    void quemNaoFezCheckInNoEventoNaoVota() throws Exception {
        // Sem participação alguma.
        votar(resumoA1, token).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_CHECKED_IN"));

        // Inscrito, mas sem presença confirmada.
        Participacao inscrito = new Participacao();
        inscrito.setUsuario(usuario);
        inscrito.setEvento(eventoA);
        inscrito.setScore(0);
        participacoes.saveAndFlush(inscrito);
        votar(resumoA1, token).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_CHECKED_IN"));
        mvc.perform(get("/evento/" + eventoA.getId() + "/votacao").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.hasCheckedIn").value(false)).andExpect(jsonPath("$.canVote").value(false));

        // Check-in em outro evento não vale para este.
        checkIn(usuario, eventoB);
        votar(resumoA1, token).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("NOT_CHECKED_IN"));
        assertEquals(0, votos.count());
    }

    @Test
    void exigeSessaoValida() throws Exception {
        checkIn(usuario, eventoA);
        mvc.perform(post("/resumos/" + resumoA1.getId() + "/voto")).andExpect(status().isUnauthorized());
        votar(resumoA1, "a".repeat(43)).andExpect(status().isUnauthorized());
        mvc.perform(get("/evento/" + eventoA.getId() + "/votacao")).andExpect(status().isUnauthorized());
        assertEquals(0, votos.count());
    }

    @Test
    void recusaResumoOuEventoInexistenteEEventoDesativado() throws Exception {
        mvc.perform(post("/resumos/999999/voto").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESUMO_NOT_FOUND"));
        mvc.perform(get("/evento/999999/votacao").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        checkIn(usuario, eventoA);
        eventoA.setAtivo(false);
        eventos.saveAndFlush(eventoA);
        votar(resumoA1, token).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EVENT_NOT_ACTIVE"));
        assertEquals(0, votos.count());
    }

    private List<Integer> votarEmParalelo(String credencial, Resumo... alvos) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(alvos.length);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (Resumo alvo : alvos) {
                futuros.add(pool.submit(() -> {
                    iniciar.await();
                    return votar(alvo, credencial).andReturn().getResponse().getStatus();
                }));
            }
            iniciar.countDown();
            List<Integer> resultados = new ArrayList<>();
            for (Future<Integer> f : futuros) resultados.add(f.get(30, TimeUnit.SECONDS));
            Collections.sort(resultados);
            return resultados;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void cliquesDuplicadosNoMesmoResumoGeramUmUnicoVoto() throws Exception {
        checkIn(usuario, eventoA);
        assertEquals(List.of(201, 409), votarEmParalelo(token, resumoA1, resumoA1));
        assertEquals(1, votos.count());
    }

    @Test
    void quatroVotosSimultaneosEmResumosDiferentesRespeitamOLimite() throws Exception {
        checkIn(usuario, eventoA);
        assertEquals(List.of(201, 201, 201, 409), votarEmParalelo(token, resumoA1, resumoA2, resumoA3, resumoA4));
        assertEquals(3, votos.count());
    }

    @Test
    void qrDoResumoSoParaAdministradorEApontaParaOResumo() throws Exception {
        mvc.perform(get("/resumos/" + resumoA1.getId() + "/qr-code").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/resumos/" + resumoA1.getId() + "/qr-code")).andExpect(status().isUnauthorized());
        mvc.perform(get("/resumos/999999/qr-code").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        String corpo = mvc.perform(get("/resumos/" + resumoA1.getId() + "/qr-code")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imageData").isString())
                .andReturn().getResponse().getContentAsString();
        var payload = json.readTree(json.readTree(corpo).get("payload").asString());
        assertEquals("RESUMO_VOTE", payload.get("type").asString());
        assertEquals(resumoA1.getId().intValue(), payload.get("resumoId").asInt());
        assertTrue(json.readTree(corpo).get("imageData").asString().startsWith("data:image/png;base64,"));
    }

    @Test
    void excluirResumoDevolveOVotoDeQuemVotouNele() throws Exception {
        checkIn(usuario, eventoA);
        votar(resumoA1, token).andExpect(status().isCreated());
        votar(resumoA2, token).andExpect(status().isCreated());
        votar(resumoA3, token).andExpect(status().isCreated());
        votar(resumoA4, token).andExpect(status().isConflict());
        resumos.deleteById(resumoA1.getId());
        assertEquals(2, votos.count());
        votar(resumoA4, token).andExpect(status().isCreated());
        assertEquals(3, votos.count());
    }
}