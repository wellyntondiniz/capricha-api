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
    @Autowired PalestraRepository palestras;
    @Autowired PresencaPalestraRepository presencasPalestra;
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
        presencasPalestra.deleteAll();
        palestras.deleteAll();
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

    private com.capricha.capricha_api.model.Palestra palestra() {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Cuiaba"));
        return palestra(agora.minusMinutes(30), agora.plusMinutes(30));
    }

    private com.capricha.capricha_api.model.Palestra palestra(LocalDateTime inicio, LocalDateTime termino) {
        var p = new com.capricha.capricha_api.model.Palestra();
        p.setNome("Palestra de teste"); p.setPalestrante("Ana"); p.setEvento(evento);
        p.setData(inicio.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        p.setHorario(inicio.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        p.setDataTermino(termino);
        return palestras.saveAndFlush(p);
    }

    private String qrPalestra(Long id) throws Exception {
        String body = mvc.perform(get("/palestras/" + id + "/qr-code").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(json.readTree(body).get("payload").asString()).get("token").asString();
    }

    private String pedidoPalestra(Long id, String qr) throws Exception {
        return json.writeValueAsString(Map.of("type", "LECTURE_CHECKIN", "lectureId", id, "qrToken", qr,
                "usuarioId", admin.getId()));
    }

    @Test
    void administradorGeraQrDePalestraEstavelSemExporTokenNaListagemComum() throws Exception {
        var p = palestra();
        String endpoint = "/palestras/" + p.getId() + "/qr-code";
        String a = mvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lectureId").value(p.getId()))
                .andReturn().getResponse().getContentAsString();
        String b = mvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var payload = json.readTree(json.readTree(a).get("payload").asString());
        assertEquals("LECTURE_CHECKIN", payload.get("type").asString());
        assertEquals(p.getId().longValue(), payload.get("lectureId").asLong());
        assertNull(payload.get("eventId"));
        assertEquals(json.readTree(a).get("payload"), json.readTree(b).get("payload"));
        assertEquals(palestras.findById(p.getId()).orElseThrow().getCheckInToken(), payload.get("token").asString());
        byte[] png = Base64.getDecoder().decode(json.readTree(a).get("imageData").asString().split(",", 2)[1]);
        var imagem = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        var source = new com.google.zxing.RGBLuminanceSource(imagem.getWidth(), imagem.getHeight(),
                imagem.getRGB(0, 0, imagem.getWidth(), imagem.getHeight(), null, 0, imagem.getWidth()));
        var bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        assertEquals(json.readTree(a).get("payload").asString(), new com.google.zxing.MultiFormatReader().decode(bitmap).getText());
        mvc.perform(get("/palestras")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].checkInToken").doesNotExist());
        assertEquals(0, presencasPalestra.count());
        assertEquals(0, participacoes.count());
    }

    @Test
    void qrDePalestraExigeSessaoEPerfilAdministrativoAtual() throws Exception {
        var p = palestra();
        String endpoint = "/palestras/" + p.getId() + "/qr-code";
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        mvc.perform(get(endpoint).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        admin.setPerfil("ADMINISTRADOR"); usuarios.saveAndFlush(admin);
        mvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
        admin.setPerfil("PARTICIPANTE"); usuarios.saveAndFlush(admin);
        mvc.perform(get(endpoint).header("Authorization", "Bearer " + adminToken)).andExpect(status().isForbidden());
    }

    @Test
    void qrDePalestraRecusaRecursoInexistenteEEventoDesativado() throws Exception {
        mvc.perform(get("/palestras/9223372036854775807/qr-code").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("LECTURE_NOT_FOUND"));
        var p = palestra();
        evento.setAtivo(false); eventos.saveAndFlush(evento);
        mvc.perform(get("/palestras/" + p.getId() + "/qr-code").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHECK_IN_NOT_AVAILABLE"));
    }

    @Test
    void identificaConfirmaERestauraPresencaDaPalestraParaUsuarioAutenticado() throws Exception {
        var p = palestra();
        String body = pedidoPalestra(p.getId(), qrPalestra(p.getId()));
        mvc.perform(post("/palestras/" + p.getId() + "/check-in/validar").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value(p.getNome())).andExpect(jsonPath("$.palestrante").value("Ana"))
                .andExpect(jsonPath("$.canCheckIn").value(true)).andExpect(jsonPath("$.hasCheckedIn").value(false))
                .andExpect(jsonPath("$.eventoId").value(evento.getId()));
        mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.lectureId").value(p.getId())).andExpect(jsonPath("$.checkedInAt").isString());
        assertTrue(presencasPalestra.findByUsuarioIdAndPalestraId(usuario.getId(), p.getId()).isPresent());
        assertTrue(presencasPalestra.findByUsuarioIdAndPalestraId(admin.getId(), p.getId()).isEmpty());
        mvc.perform(get("/palestras/check-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].hasCheckedIn").value(true));
        mvc.perform(get("/palestras/check-in").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].hasCheckedIn").value(false));
        mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CHECKED_IN"));
    }

    @Test
    void rejeitaTipoIdOuTokenDeOutroRecursoNoBackend() throws Exception {
        var a = palestra();
        var b = palestra();
        String tokenA = qrPalestra(a.getId());
        String tokenB = qrPalestra(b.getId());
        for (String body : List.of(
                json.writeValueAsString(Map.of("type", "EVENT_CHECKIN", "lectureId", a.getId(), "qrToken", tokenA)),
                pedidoPalestra(b.getId(), tokenA))) {
            mvc.perform(post("/palestras/" + a.getId() + "/check-in").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_QR"));
        }
        for (String incorreto : List.of(tokenB, qrToken(), UUID.randomUUID().toString())) {
            mvc.perform(post("/palestras/" + a.getId() + "/check-in/validar").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(pedidoPalestra(a.getId(), incorreto)))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_LECTURE_TOKEN"));
            mvc.perform(post("/palestras/" + a.getId() + "/check-in").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(pedidoPalestra(a.getId(), incorreto)))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(0, presencasPalestra.count());
    }

    @Test
    void recusaPalestraAntesDoInicioDepoisDoTerminoESemDataValida() throws Exception {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Cuiaba"));
        var futura = palestra(agora.plusHours(1), agora.plusHours(2));
        var encerrada = palestra(agora.minusHours(2), agora.minusHours(1));
        var invalida = palestra();
        invalida.setData("31/02/2026"); palestras.saveAndFlush(invalida);
        for (var p : List.of(futura, encerrada, invalida)) {
            String body = pedidoPalestra(p.getId(), qrPalestra(p.getId()));
            mvc.perform(post("/palestras/" + p.getId() + "/check-in/validar").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.canCheckIn").value(false));
            mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("CHECK_IN_NOT_AVAILABLE"));
        }
        assertEquals(0, presencasPalestra.count());
    }

    @Test
    void palestraAntigaSemTerminoMantemDadosEEventoDesativadoImpedeNovaPresenca() throws Exception {
        var p = palestra(LocalDateTime.now(ZoneId.of("America/Cuiaba")).minusMinutes(30), null);
        String body = pedidoPalestra(p.getId(), qrPalestra(p.getId()));
        mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        assertNull(palestras.findById(p.getId()).orElseThrow().getDataTermino());
        evento.setAtivo(false); eventos.saveAndFlush(evento);
        mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
    }

    @Test
    void presencasDeEventoEPalestrasSaoIndependentesEScorePermaneceIntacto() throws Exception {
        Participacao inscricao = new Participacao();
        inscricao.setUsuario(usuario); inscricao.setEvento(evento); inscricao.setScore(90);
        Integer inscricaoId = participacoes.saveAndFlush(inscricao).getId();
        var a = palestra(); var b = palestra();
        mvc.perform(post("/palestras/" + a.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedidoPalestra(a.getId(), qrPalestra(a.getId()))))
                .andExpect(status().isCreated());
        assertNull(participacoes.findById(inscricaoId).orElseThrow().getCheckedInAt());
        assertEquals(90, participacoes.findById(inscricaoId).orElseThrow().getScore());
        assertTrue(presencasPalestra.findByUsuarioIdAndPalestraId(usuario.getId(), b.getId()).isEmpty());
        mvc.perform(post("/evento/" + evento.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedido(qrToken()))).andExpect(status().isCreated());
        assertTrue(presencasPalestra.findByUsuarioIdAndPalestraId(usuario.getId(), b.getId()).isEmpty());
        mvc.perform(post("/palestras/" + b.getId() + "/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedidoPalestra(b.getId(), qrPalestra(b.getId()))))
                .andExpect(status().isCreated());
        assertEquals(2, presencasPalestra.count());
        assertEquals(1, participacoes.count());
        assertEquals(90, participacoes.findById(inscricaoId).orElseThrow().getScore());
    }

    @Test
    void duasConfirmacoesConcorrentesDaPalestraGeramUmaPresencaEIndiceUnicoImpedeOutra() throws Exception {
        var p = palestra();
        String body = pedidoPalestra(p.getId(), qrPalestra(p.getId()));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        Callable<Integer> confirmar = () -> {
            iniciar.await();
            return mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
        };
        try {
            var a = pool.submit(confirmar); var b = pool.submit(confirmar); iniciar.countDown();
            var status = new ArrayList<>(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)));
            Collections.sort(status); assertEquals(List.of(201, 409), status);
            assertEquals(1, presencasPalestra.count());
        } finally { pool.shutdownNow(); }
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                presencasPalestra.saveAndFlush(new PresencaPalestra(usuario, p, Instant.now())));
    }

    @Test
    void registroDePalestraExigeCredencialRealEPalestraExistente() throws Exception {
        var p = palestra();
        String body = pedidoPalestra(p.getId(), qrPalestra(p.getId()));
        mvc.perform(post("/palestras/" + p.getId() + "/check-in").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/palestras/" + p.getId() + "/check-in").header("Authorization", "Bearer " + "a".repeat(43))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/palestras/9223372036854775807/check-in").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(pedidoPalestra(Long.MAX_VALUE, UUID.randomUUID().toString())))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("LECTURE_NOT_FOUND"));
        assertEquals(0, presencasPalestra.count());
    }

    @Test
    void cadastroAceitaTerminoOpcionalERecusaFimAnteriorOuForaDoEvento() throws Exception {
        var inicio = LocalDateTime.now(ZoneId.of("America/Cuiaba")).plusMinutes(10).withSecond(0).withNano(0);
        var fim = inicio.plusMinutes(10);
        String data = inicio.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horario = inicio.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        mvc.perform(multipart("/palestras").param("nome", "Com término").param("descricao", "Descrição")
                .param("palestrante", "Ana").param("data", data).param("horario", horario)
                .param("evento", evento.getId().toString()).param("dataTermino", fim.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataTermino").isString());
        mvc.perform(multipart("/palestras").param("nome", "Sem término").param("descricao", "Descrição")
                .param("palestrante", "Bruno").param("data", data).param("horario", horario)
                .param("evento", evento.getId().toString())).andExpect(status().isOk());
        for (String termino : List.of(inicio.minusMinutes(1).toString(), inicio.plusDays(1).toString(), "inválido")) {
            mvc.perform(multipart("/palestras").param("nome", "Inválida").param("descricao", "Descrição")
                    .param("palestrante", "Carla").param("data", data).param("horario", horario)
                    .param("evento", evento.getId().toString()).param("dataTermino", termino))
                    .andExpect(status().isBadRequest());
        }
        assertEquals(2, palestras.count());
    }
}
