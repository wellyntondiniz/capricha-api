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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.capricha.capricha_api.entidade.*;
import com.capricha.capricha_api.repository.*;
import com.capricha.capricha_api.service.Senhas;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class CheckInIntegracaoTest {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired EventoRepository eventos;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private Usuario usuario;
    private Usuario admin;
    private Evento evento;
    private String token;
    private String adminToken;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        sessoes.deleteAll();
        participacoes.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
        usuario = usuario("participante@teste.com", "PARTICIPANTE");
        admin = usuario("admin@teste.com", "ADMIN");
        evento = new Evento();
        evento.setNome("Evento real de teste");
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Cuiaba"));
        evento.setDataInicio(agora.minusHours(1));
        evento.setDataTermino(agora.plusHours(1));
        evento = eventos.saveAndFlush(evento);
        token = login(usuario);
        adminToken = login(admin);
    }

    private Usuario usuario(String email, String perfil) {
        Usuario u = new Usuario();
        u.setNome(perfil);
        u.setEmail(email);
        u.setSenha(Senhas.proteger("Senha@123"));
        u.setPerfil(perfil);
        return usuarios.saveAndFlush(u);
    }

    private String login(Usuario u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "Senha@123"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String credencial = json.readTree(body).get("token").asString();
        assertEquals(43, credencial.length());
        return credencial;
    }

    private String qrToken() throws Exception {
        String body = mvc.perform(get("/evento/" + evento.getId() + "/qr-code")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.imageData").isString())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(json.readTree(body).get("payload").asString()).get("token").asString();
    }

    private String pedido(String qr) throws Exception {
        return json.writeValueAsString(Map.of("qrToken", qr, "usuarioId", admin.getId()));
    }

    @Test
    void loginPrincipalIdentificaConfirmaEPersisteSemAceitarIdentidadeDoBody() throws Exception {
        String qr = qrToken();
        mvc.perform(get("/auth/sessao").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(usuario.getId()));
        mvc.perform(post("/evento/" + evento.getId() + "/check-in/validar")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(pedido(qr)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value(evento.getNome()))
                .andExpect(jsonPath("$.hasCheckedIn").value(false));
        mvc.perform(post("/evento/" + evento.getId() + "/check-in")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(pedido(qr)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.checkedInAt").isString());
        var salvos = participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), evento.getId());
        assertEquals(1, salvos.size());
        assertNotNull(salvos.get(0).getCheckedInAt());
        assertTrue(participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(admin.getId(), evento.getId()).isEmpty());
        mvc.perform(get("/evento/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].hasCheckedIn").value(true));
        mvc.perform(get("/evento/" + evento.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.checkInToken").doesNotExist());
    }

    @Test
    void preservaInscricaoPontuacaoEDadosHistoricosERecusaRepeticao() throws Exception {
        Participacao p = new Participacao();
        p.setUsuario(usuario); p.setEvento(evento); p.setScore(90);
        p = participacoes.saveAndFlush(p);
        String qr = qrToken();
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qr))).andExpect(status().isCreated());
        var salvo = participacoes.findById(p.getId()).orElseThrow();
        assertEquals(90, salvo.getScore());
        assertNotNull(salvo.getCheckedInAt());
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qr)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_CHECKED_IN"));
        // O endpoint antigo de inscrição não pode apagar a presença nem zerar a pontuação.
        mvc.perform(post("/participacao").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("id", p.getId(), "usuario", Map.of("id", usuario.getId()),
                        "evento", Map.of("id", evento.getId()), "checkedInAt", Instant.now().toString(), "score", 0))))
                .andExpect(status().isOk());
        assertEquals(salvo.getCheckedInAt(), participacoes.findById(p.getId()).orElseThrow().getCheckedInAt());
        assertEquals(90, participacoes.findById(p.getId()).orElseThrow().getScore());
    }

    @Test
    void duasConfirmacoesConcorrentesGeramUmaUnicaPresenca() throws Exception {
        String body = pedido(qrToken());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        Callable<Integer> confirmar = () -> {
            iniciar.await();
            return mvc.perform(post("/evento/" + evento.getId() + "/check-in")
                    .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andReturn().getResponse().getStatus();
        };
        try {
            var a = pool.submit(confirmar);
            var b = pool.submit(confirmar);
            iniciar.countDown();
            var resultados = new ArrayList<>(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
            Collections.sort(resultados);
            assertEquals(List.of(201, 409), resultados);
            assertEquals(1, participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), evento.getId()).size());
        } finally { pool.shutdownNow(); }
    }

    @Test
    void rejeitaQrInvalidoEventoInexistenteEForaDaJanelaSemGravar() throws Exception {
        String qr = qrToken();
        mvc.perform(post("/evento/" + evento.getId() + "/check-in/validar")
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(pedido(UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_EVENT_TOKEN"));
        mvc.perform(post("/evento/2147483647/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qr)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("EVENT_NOT_FOUND"));
        evento = eventos.findById(evento.getId()).orElseThrow();
        evento.setDataInicio(LocalDateTime.now(ZoneId.of("America/Cuiaba")).plusDays(1));
        evento.setDataTermino(evento.getDataInicio().plusHours(1));
        eventos.saveAndFlush(evento);
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qr)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHECK_IN_NOT_AVAILABLE"));
        evento.setAtivo(false); eventos.saveAndFlush(evento);
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qr))).andExpect(status().isConflict());
        assertEquals(0, participacoes.count());
    }

    @Test
    void exigeCredencialRealEPermissaoDoBanco() throws Exception {
        mvc.perform(get("/evento/check-in")).andExpect(status().isUnauthorized());
        mvc.perform(get("/evento/check-in").header("Authorization", "Bearer " + "a".repeat(43)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/evento/" + evento.getId() + "/qr-code").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        usuario.setAtivo(false); usuarios.saveAndFlush(usuario);
        mvc.perform(get("/evento/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevogaCredencialETrocaDeSenhaInvalidaSessoes() throws Exception {
        mvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        mvc.perform(get("/evento/check-in").header("Authorization", "Bearer " + token)).andExpect(status().isUnauthorized());
        String nova = login(usuario);
        usuario.setSenha(Senhas.proteger("Outra@123")); usuarios.saveAndFlush(usuario);
        mvc.perform(get("/auth/sessao").header("Authorization", "Bearer " + nova)).andExpect(status().isUnauthorized());
    }

    @Test
    void credencialExpiradaNaoAutorizaCheckIn() throws Exception {
        jdbc.update("update sessao_usuario set expira_em = ? where usuario_id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), usuario.getId());
        mvc.perform(get("/evento/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertTrue(sessoes.findById(token).isEmpty(), "O token original não deve ser armazenado no banco");
    }

    @Test
    void qrGeradoPodeSerDecodificadoEPermissaoAtualDoBancoPrevalece() throws Exception {
        String body = mvc.perform(get("/evento/" + evento.getId() + "/qr-code")
                .header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var resposta = json.readTree(body);
        byte[] png = Base64.getDecoder().decode(resposta.get("imageData").asString().split(",", 2)[1]);
        var imagem = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        int[] pixels = imagem.getRGB(0, 0, imagem.getWidth(), imagem.getHeight(), null, 0, imagem.getWidth());
        var source = new com.google.zxing.RGBLuminanceSource(imagem.getWidth(), imagem.getHeight(), pixels);
        var bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        assertEquals(resposta.get("payload").asString(), new com.google.zxing.MultiFormatReader().decode(bitmap).getText());
        admin.setPerfil("PARTICIPANTE"); usuarios.saveAndFlush(admin);
        mvc.perform(get("/evento/" + evento.getId() + "/qr-code").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void participacoesHistoricasDuplicadasNaoSaoExcluidasNemGeramDuasPresencas() throws Exception {
        for (int score : List.of(15, 25)) {
            Participacao p = new Participacao();
            p.setUsuario(usuario); p.setEvento(evento); p.setScore(score);
            participacoes.saveAndFlush(p);
        }
        String body = pedido(qrToken());
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        var salvos = participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(usuario.getId(), evento.getId());
        assertEquals(2, salvos.size());
        assertEquals(1, salvos.stream().filter(p -> p.getCheckedInAt() != null).count());
        assertEquals(List.of(15, 25), salvos.stream().map(Participacao::getScore).toList());
    }

    @Test
    void atualizacaoDeScoreConcorrentePreservaPresencaEPontuacao() throws Exception {
        Participacao p = new Participacao();
        p.setUsuario(usuario); p.setEvento(evento); p.setScore(40);
        Integer id = participacoes.saveAndFlush(p).getId();
        String body = pedido(qrToken());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        try {
            var presenca = pool.submit(() -> {
                iniciar.await();
                return mvc.perform(post("/evento/" + evento.getId() + "/check-in")
                        .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andReturn().getResponse().getStatus();
            });
            var score = pool.submit(() -> {
                iniciar.await();
                return mvc.perform(put("/participacao/usuario/" + usuario.getId() + "/evento/" + evento.getId() + "/score/10"))
                        .andReturn().getResponse().getStatus();
            });
            iniciar.countDown();
            assertEquals(201, presenca.get(20, TimeUnit.SECONDS));
            assertEquals(200, score.get(20, TimeUnit.SECONDS));
            var salvo = participacoes.findById(id).orElseThrow();
            assertNotNull(salvo.getCheckedInAt());
            assertEquals(50, salvo.getScore());
        } finally { pool.shutdownNow(); }
    }
}
