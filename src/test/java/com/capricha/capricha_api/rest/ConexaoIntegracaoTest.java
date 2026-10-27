package com.capricha.capricha_api.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.capricha.capricha_api.entidade.*;
import com.capricha.capricha_api.repository.*;
import com.capricha.capricha_api.service.Senhas;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class ConexaoIntegracaoTest {
    private static final String AUTH = "Authorization";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired EventoRepository eventos;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired PalestraRepository palestras;
    @Autowired PresencaPalestraRepository presencasPalestra;
    @Autowired ConexaoParticipantesRepository conexoes;
    @Autowired CodigoConexaoRepository codigos;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private Usuario ana, bruno, carla;
    private String tokenAna, tokenBruno, tokenCarla, qrAna, qrBruno;
    private Evento evento;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        limparTudo();
        ana = usuario("ana@teste.com", "Ana");
        bruno = usuario("bruno@teste.com", "Bruno");
        carla = usuario("carla@teste.com", "Carla");
        evento = evento("Evento de conexões", Duration.ofHours(-1), Duration.ofHours(1));
        presenca(ana, evento);
        presenca(bruno, evento);
        presenca(carla, evento);
        tokenAna = login(ana);
        tokenBruno = login(bruno);
        tokenCarla = login(carla);
        qrAna = qrDe(tokenAna);
        qrBruno = qrDe(tokenBruno);
    }

    // As tabelas de conexão referenciam usuário e evento. Se ficassem preenchidas, a limpeza
    // dos outros testes (que apagam usuários e eventos) falharia por chave estrangeira.
    @AfterEach
    void limparConexoes() {
        conexoes.deleteAll();
        codigos.deleteAll();
    }

    private void limparTudo() {
        conexoes.deleteAll();
        codigos.deleteAll();
        presencasPalestra.deleteAll();
        palestras.deleteAll();
        sessoes.deleteAll();
        participacoes.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
    }

    private Usuario usuario(String email, String nome) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(Senhas.proteger("Senha@123"));
        u.setPerfil("PARTICIPANTE");
        return usuarios.saveAndFlush(u);
    }

    private Evento evento(String nome, Duration inicio, Duration termino) {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Cuiaba"));
        Evento e = new Evento();
        e.setNome(nome);
        e.setDataInicio(agora.plus(inicio));
        e.setDataTermino(agora.plus(termino));
        return eventos.saveAndFlush(e);
    }

    private void presenca(Usuario u, Evento e) {
        Participacao p = new Participacao();
        p.setUsuario(u);
        p.setEvento(e);
        p.setScore(0);
        p.setCheckedInAt(Instant.now());
        participacoes.saveAndFlush(p);
    }

    private int score(Usuario u, Evento e) {
        return participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(u.getId(), e.getId()).get(0).getScore();
    }

    private String login(Usuario u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "Senha@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asString();
    }

    private String qrDe(String token) throws Exception {
        String body = mvc.perform(get("/conexoes/meu-qr").header(AUTH, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imageData").isString())
                .andReturn().getResponse().getContentAsString();
        String payload = json.readTree(body).get("payload").asString();
        assertEquals("USER_CONNECT", json.readTree(payload).get("type").asString());
        return json.readTree(payload).get("token").asString();
    }

    private String pedido(String qr, Evento e) {
        return json.writeValueAsString(Map.of("qrToken", qr, "eventoId", e.getId()));
    }

    private ResultActions solicitar(String token, String qr, Evento e) throws Exception {
        return mvc.perform(post("/conexoes/solicitacoes").header(AUTH, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qr, e)));
    }

    private long solicitarOk(String token, String qr, Evento e) throws Exception {
        String body = solicitar(token, qr, e).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").longValue();
    }

    private ResultActions aceitar(String token, long id) throws Exception {
        return mvc.perform(post("/conexoes/solicitacoes/" + id + "/aceitar").header(AUTH, "Bearer " + token));
    }

    private ResultActions recusar(String token, long id) throws Exception {
        return mvc.perform(post("/conexoes/solicitacoes/" + id + "/recusar").header(AUTH, "Bearer " + token));
    }

    @Test
    void fluxoCompletoSoPontuaNoAceiteEOsDoisRecebemXp() throws Exception {
        // O QR é estável e não carrega id nem e-mail.
        assertEquals(qrAna, qrDe(tokenAna));
        mvc.perform(post("/conexoes/identificar").header(AUTH, "Bearer " + tokenBruno)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qrAna, evento)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Ana"))
                .andExpect(jsonPath("$.relacao").value("NONE")).andExpect(jsonPath("$.email").doesNotExist());

        long id = solicitarOk(tokenBruno, qrAna, evento);
        // Pendente não gera XP para ninguém.
        assertEquals(0, score(ana, evento));
        assertEquals(0, score(bruno, evento));
        mvc.perform(get("/conexoes").header(AUTH, "Bearer " + tokenAna))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recebidas.length()").value(1))
                .andExpect(jsonPath("$.recebidas[0].nome").value("Bruno"));
        mvc.perform(get("/conexoes").header(AUTH, "Bearer " + tokenBruno))
                .andExpect(jsonPath("$.enviadas.length()").value(1));

        aceitar(tokenAna, id).andExpect(status().isOk()).andExpect(jsonPath("$.xp").value(150));
        assertEquals(150, score(ana, evento));
        assertEquals(150, score(bruno, evento));
        // Reflete no ranking existente.
        mvc.perform(get("/participacao/" + evento.getId() + "/byEvento"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].score").value(150));
        mvc.perform(get("/conexoes").header(AUTH, "Bearer " + tokenBruno))
                .andExpect(jsonPath("$.conexoes.length()").value(1)).andExpect(jsonPath("$.conexoes[0].nome").value("Ana"))
                .andExpect(jsonPath("$.enviadas.length()").value(0));

        // Nova tentativa, em qualquer direção, não pontua de novo.
        aceitar(tokenAna, id).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REQUEST_NOT_PENDING"));
        solicitar(tokenBruno, qrAna, evento).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_CONNECTED"));
        solicitar(tokenAna, qrBruno, evento).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_CONNECTED"));
        assertEquals(150, score(ana, evento));
        assertEquals(150, score(bruno, evento));
    }

    @Test
    void naoPermiteConectarConsigoMesmo() throws Exception {
        solicitar(tokenAna, qrAna, evento).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SELF_CONNECTION"));
        assertEquals(0, conexoes.count());
    }

    @Test
    void solicitacaoDuplicadaOuInversaEhRecusadaEnquantoPendente() throws Exception {
        solicitarOk(tokenBruno, qrAna, evento);
        solicitar(tokenBruno, qrAna, evento).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONNECTION_PENDING"));
        solicitar(tokenAna, qrBruno, evento).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONNECTION_PENDING"));
        assertEquals(1, conexoes.count());
    }

    @Test
    void recusarNaoDaXpEPermiteNovaSolicitacao() throws Exception {
        long id = solicitarOk(tokenBruno, qrAna, evento);
        recusar(tokenAna, id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("RECUSADA"));
        assertEquals(0, score(ana, evento));
        assertEquals(0, score(bruno, evento));
        aceitar(tokenAna, id).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("REQUEST_NOT_PENDING"));
        long novo = solicitarOk(tokenBruno, qrAna, evento);
        aceitar(tokenAna, novo).andExpect(status().isOk());
        assertEquals(150, score(ana, evento));
    }

    @Test
    void soODestinatarioRespondeEOutrosNaoEnxergamASolicitacao() throws Exception {
        long id = solicitarOk(tokenBruno, qrAna, evento);
        aceitar(tokenBruno, id).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("REQUEST_NOT_FOUND"));
        aceitar(tokenCarla, id).andExpect(status().isNotFound());
        recusar(tokenCarla, id).andExpect(status().isNotFound());
        assertEquals(0, score(ana, evento));
        assertEquals(0, score(bruno, evento));
    }

    @Test
    void exigeCheckInDosDoisEEventoEmAndamento() throws Exception {
        Evento outro = evento("Outro evento", Duration.ofHours(-1), Duration.ofHours(1));
        presenca(bruno, outro); // só Bruno tem check-in
        solicitar(tokenBruno, qrAna, outro).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PEER_NOT_CHECKED_IN"));
        solicitar(tokenAna, qrBruno, outro).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHECK_IN_REQUIRED"));

        Evento encerrado = evento("Encerrado", Duration.ofHours(-3), Duration.ofHours(-1));
        presenca(ana, encerrado);
        presenca(bruno, encerrado);
        solicitar(tokenBruno, qrAna, encerrado).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONNECTION_NOT_AVAILABLE"));
        assertEquals(0, conexoes.count());
    }

    @Test
    void mesmaDuplaPontuaDeNovoEmOutroEvento() throws Exception {
        aceitar(tokenAna, solicitarOk(tokenBruno, qrAna, evento)).andExpect(status().isOk());
        Evento outro = evento("Segundo evento", Duration.ofHours(-1), Duration.ofHours(2));
        presenca(ana, outro);
        presenca(bruno, outro);
        aceitar(tokenAna, solicitarOk(tokenBruno, qrAna, outro)).andExpect(status().isOk());
        assertEquals(150, score(ana, evento));
        assertEquals(150, score(ana, outro));
        assertEquals(150, score(bruno, outro));
    }

    @Test
    void qrInexistenteERequisicaoSemSessaoSaoRecusados() throws Exception {
        solicitar(tokenBruno, UUID.randomUUID().toString(), evento)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        mvc.perform(get("/conexoes/meu-qr")).andExpect(status().isUnauthorized());
        mvc.perform(post("/conexoes/solicitacoes").contentType(MediaType.APPLICATION_JSON).content(pedido(qrAna, evento)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duasAceitacoesSimultaneasConcedemXpUmaUnicaVez() throws Exception {
        long id = solicitarOk(tokenBruno, qrAna, evento);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch largada = new CountDownLatch(1);
        Callable<Integer> tarefa = () -> {
            largada.await();
            return aceitar(tokenAna, id).andReturn().getResponse().getStatus();
        };
        Future<Integer> a = pool.submit(tarefa);
        Future<Integer> b = pool.submit(tarefa);
        largada.countDown();
        List<Integer> respostas = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(1, respostas.stream().filter(s -> s == 200).count(), respostas.toString());
        assertEquals(1, respostas.stream().filter(s -> s == 409).count(), respostas.toString());
        assertEquals(150, score(ana, evento));
        assertEquals(150, score(bruno, evento));
    }
}