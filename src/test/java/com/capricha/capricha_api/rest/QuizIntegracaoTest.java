package com.capricha.capricha_api.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Participacao;
import com.capricha.capricha_api.entidade.PresencaPalestra;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.EventoRepository;
import com.capricha.capricha_api.repository.PalestraRepository;
import com.capricha.capricha_api.repository.ParticipacaoRepository;
import com.capricha.capricha_api.repository.PresencaPalestraRepository;
import com.capricha.capricha_api.repository.QuizRepository;
import com.capricha.capricha_api.repository.RespostaQuizRepository;
import com.capricha.capricha_api.repository.SessaoUsuarioRepository;
import com.capricha.capricha_api.repository.UsuarioRepository;
import com.capricha.capricha_api.service.Senhas;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class QuizIntegracaoTest {
    private static final MediaType JSON = MediaType.APPLICATION_JSON;

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired EventoRepository eventos;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired PalestraRepository palestras;
    @Autowired PresencaPalestraRepository presencas;
    @Autowired QuizRepository quizzes;
    @Autowired RespostaQuizRepository respostas;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private Usuario aluno;
    private Usuario admin;
    private Evento evento;
    private Palestra palestra;
    private String token;
    private String adminToken;
    // Perguntas cadastradas na palestra: p1 tem uma correta; p2 tem duas corretas.
    private JsonNode p1;
    private JsonNode p2;
    private Long quizId;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        limpar();
        aluno = usuario("aluno@teste.com", "PARTICIPANTE");
        admin = usuario("admin@teste.com", "ADMIN");
        evento = new Evento();
        evento.setNome("Evento do quiz");
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Cuiaba"));
        evento.setDataInicio(agora.minusHours(1));
        evento.setDataTermino(agora.plusHours(2));
        evento = eventos.saveAndFlush(evento);
        palestra = novaPalestra();
        p1 = pergunta("Qual é a capital do Brasil?", new String[] { "Brasília", "Rio", "Cuiabá", "Salvador" }, 0);
        p2 = pergunta("Quais são linguagens JVM?", new String[] { "Java", "Kotlin", "Python", "Ruby" }, 0, 1);
        token = login(aluno);
        adminToken = login(admin);
        quizId = criarQuiz(100);
    }

    @AfterEach
    void limpar() {
        // Quizzes referenciam palestras: precisam sair antes para não atrapalhar os demais testes.
        respostas.deleteAll();
        quizzes.deleteAll();
        presencas.deleteAll();
        palestras.deleteAll();
        sessoes.deleteAll();
        participacoes.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
    }

    // ---------- apoio ----------

    private Usuario usuario(String email, String perfil) {
        Usuario u = new Usuario();
        u.setNome(perfil);
        u.setEmail(email);
        u.setSenha(Senhas.proteger("Senha@123"));
        u.setPerfil(perfil);
        return usuarios.saveAndFlush(u);
    }

    private String login(Usuario u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "Senha@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asString();
    }

    private Palestra novaPalestra() {
        LocalDateTime agora = LocalDateTime.now(ZoneId.of("America/Cuiaba")).minusMinutes(30);
        Palestra p = new Palestra();
        p.setNome("Palestra do quiz");
        p.setPalestrante("Ana");
        p.setEvento(evento);
        p.setData(agora.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        p.setHorario(agora.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        p.setDataTermino(agora.plusHours(1));
        return palestras.saveAndFlush(p);
    }

    /** Cadastra a pergunta pelo mesmo endpoint usado pela tela de cadastro e devolve o JSON com os ids. */
    private JsonNode pergunta(String enunciado, String[] textos, int... corretas) throws Exception {
        List<Map<String, Object>> alternativas = new ArrayList<>();
        for (int i = 0; i < textos.length; i++) {
            boolean correta = false;
            for (int c : corretas) correta |= c == i;
            alternativas.add(Map.of("texto", textos[i], "correta", correta));
        }
        String body = mvc.perform(post("/palestras/" + palestra.getId() + "/add-pergunta").contentType(JSON)
                .content(json.writeValueAsString(Map.of("enunciado", enunciado, "alternativas", alternativas))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private Long criarQuiz(int pontos) throws Exception {
        String body = mvc.perform(post("/quizzes").header("Authorization", "Bearer " + adminToken).contentType(JSON)
                .content(json.writeValueAsString(Map.of("titulo", "Quiz da palestra", "descricao", "Teste",
                        "pontuacao", pontos, "palestraId", palestra.getId()))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.totalPerguntas").value(2))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private void checkInEvento() {
        Participacao p = new Participacao();
        p.setUsuario(aluno); p.setEvento(evento); p.setScore(0); p.setCheckedInAt(Instant.now());
        participacoes.saveAndFlush(p);
    }

    private void checkInPalestra() {
        presencas.saveAndFlush(new PresencaPalestra(aluno, palestra, Instant.now()));
    }

    private int pontos() {
        return participacoes.findByUsuarioIdAndEventoIdOrderByIdAsc(aluno.getId(), evento.getId()).get(0).getScore();
    }

    private int idAlternativa(JsonNode pergunta, int indice) {
        return pergunta.get("alternativas").get(indice).get("id").asInt();
    }

    private Map<String, Object> resposta(JsonNode pergunta, int... indices) {
        List<Integer> ids = new ArrayList<>();
        for (int i : indices) ids.add(idAlternativa(pergunta, i));
        return item(pergunta.get("id").asInt(), ids);
    }

    private Map<String, Object> item(int perguntaId, List<Integer> alternativaIds) {
        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("perguntaId", perguntaId);
        corpo.put("alternativaIds", alternativaIds);
        return corpo;
    }

    private org.springframework.test.web.servlet.ResultActions enviar(String credencial, List<Map<String, Object>> itens)
            throws Exception {
        var req = post("/quizzes/" + quizId + "/respostas").contentType(JSON)
                .content(json.writeValueAsString(Map.of("respostas", itens)));
        if (credencial != null) req = req.header("Authorization", "Bearer " + credencial);
        return mvc.perform(req);
    }

    private List<Map<String, Object>> gabaritoCompleto() {
        return List.of(resposta(p1, 0), resposta(p2, 0, 1));
    }

    // ---------- listagem e bloqueios ----------

    @Test
    void listagemExigeSessao() throws Exception {
        mvc.perform(get("/quizzes")).andExpect(status().isUnauthorized());
        mvc.perform(get("/quizzes/respostas")).andExpect(status().isUnauthorized());
        enviar(null, gabaritoCompleto()).andExpect(status().isUnauthorized());
    }

    @Test
    void semCheckInNoEventoOQuizFicaBloqueadoSemExporPerguntas() throws Exception {
        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponivel").value(false))
                .andExpect(jsonPath("$[0].bloqueio").value("EVENT_CHECK_IN_REQUIRED"))
                .andExpect(jsonPath("$[0].perguntas").isEmpty())
                .andExpect(jsonPath("$[0].totalPerguntas").value(2));
        enviar(token, gabaritoCompleto()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EVENT_CHECK_IN_REQUIRED"));
        assertEquals(0, respostas.count());
    }

    @Test
    void comEventoMasSemCheckInNaPalestraEspecificaOQuizContinuaBloqueado() throws Exception {
        checkInEvento();
        // Presença em OUTRA palestra do mesmo evento não vale para este quiz.
        Palestra outra = novaPalestra();
        outra.setNome("Outra palestra");
        outra.setPalestrante("Bruno");
        palestras.saveAndFlush(outra);
        presencas.saveAndFlush(new PresencaPalestra(aluno, outra, Instant.now()));

        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponivel").value(false))
                .andExpect(jsonPath("$[0].bloqueio").value("LECTURE_CHECK_IN_REQUIRED"))
                .andExpect(jsonPath("$[0].perguntas").isEmpty());
        enviar(token, gabaritoCompleto()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("LECTURE_CHECK_IN_REQUIRED"));
        assertEquals(0, respostas.count());
        assertEquals(0, pontos());
    }

    @Test
    void liberadoAposOsDoisCheckInsENuncaExpoeOGabarito() throws Exception {
        checkInEvento();
        checkInPalestra();
        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponivel").value(true))
                .andExpect(jsonPath("$[0].bloqueio").doesNotExist())
                .andExpect(jsonPath("$[0].perguntas.length()").value(2))
                .andExpect(jsonPath("$[0].perguntas[0].alternativas.length()").value(4))
                .andExpect(jsonPath("$[0].perguntas[0].alternativas[0].correta").doesNotExist());
    }

    @Test
    void quizDeEventoDesativadoNaoApareceNemAceitaResposta() throws Exception {
        checkInEvento();
        checkInPalestra();
        evento.setAtivo(false);
        eventos.saveAndFlush(evento);
        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        enviar(token, gabaritoCompleto()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EVENT_NOT_ACTIVE"));
        assertEquals(0, respostas.count());
    }

    // ---------- correção e pontos ----------

    @Test
    void acertarTudoSomaAPontuacaoNoRankingUmaUnicaVez() throws Exception {
        checkInEvento();
        checkInPalestra();
        enviar(token, gabaritoCompleto()).andExpect(status().isCreated())
                .andExpect(jsonPath("$.acertos").value(2)).andExpect(jsonPath("$.totalPerguntas").value(2))
                .andExpect(jsonPath("$.pontosObtidos").value(100));
        assertEquals(100, pontos());
        mvc.perform(get("/participacao/" + evento.getId() + "/byEvento")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].score").value(100));
        mvc.perform(get("/quizzes/respostas").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].pontosObtidos").value(100));
        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].respondido").value(true)).andExpect(jsonPath("$[0].disponivel").value(false));

        enviar(token, gabaritoCompleto()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_ANSWERED"));
        assertEquals(100, pontos());
        assertEquals(1, respostas.count());
    }

    @Test
    void acertoParcialDaPontosProporcionaisEPerguntaDeVariasCorretasExigeTodas() throws Exception {
        checkInEvento();
        checkInPalestra();
        // p1 certa; p2 só com uma das duas corretas marcadas conta como errada.
        enviar(token, List.of(resposta(p1, 0), resposta(p2, 0))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.acertos").value(1)).andExpect(jsonPath("$.pontosObtidos").value(50));
        assertEquals(50, pontos());
    }

    @Test
    void errarTudoRegistraATentativaSemPontos() throws Exception {
        checkInEvento();
        checkInPalestra();
        enviar(token, List.of(resposta(p1, 1), resposta(p2, 2, 3))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.acertos").value(0)).andExpect(jsonPath("$.pontosObtidos").value(0));
        assertEquals(0, pontos());
        assertEquals(1, respostas.count());
    }

    @Test
    void marcarCorretaMaisUmaErradaNaoContaComoAcerto() throws Exception {
        checkInEvento();
        checkInPalestra();
        enviar(token, List.of(resposta(p1, 0, 1), resposta(p2, 0, 1))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.acertos").value(1));
    }

    // ---------- validação do envio ----------

    @Test
    void envioInvalidoEhRecusadoSemGravarNemPontuar() throws Exception {
        checkInEvento();
        checkInPalestra();
        // faltando uma pergunta
        enviar(token, List.of(resposta(p1, 0))).andExpect(status().isBadRequest());
        // sem alternativa marcada
        enviar(token, List.of(resposta(p1, 0), item(p2.get("id").asInt(), List.of())))
                .andExpect(status().isBadRequest());
        // alternativa de outra pergunta
        enviar(token, List.of(resposta(p1, 0),
                item(p2.get("id").asInt(), List.of(idAlternativa(p1, 0)))))
                .andExpect(status().isBadRequest());
        // pergunta que não pertence ao quiz
        enviar(token, List.of(resposta(p1, 0), item(999999, List.of(1))))
                .andExpect(status().isBadRequest());
        // mesma pergunta duas vezes
        enviar(token, List.of(resposta(p1, 0), resposta(p1, 0))).andExpect(status().isBadRequest());
        // lista vazia
        enviar(token, List.of()).andExpect(status().isBadRequest());
        // corpo sem a lista
        mvc.perform(post("/quizzes/" + quizId + "/respostas").header("Authorization", "Bearer " + token)
                .contentType(JSON).content("{}")).andExpect(status().isBadRequest());
        assertEquals(0, respostas.count());
        assertEquals(0, pontos());
        // Depois de tudo isso o participante ainda pode enviar a resposta correta.
        enviar(token, gabaritoCompleto()).andExpect(status().isCreated());
    }

    @Test
    void quizInexistenteRetorna404() throws Exception {
        checkInEvento();
        checkInPalestra();
        mvc.perform(post("/quizzes/987654/respostas").header("Authorization", "Bearer " + token).contentType(JSON)
                .content(json.writeValueAsString(Map.of("respostas", gabaritoCompleto()))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("QUIZ_NOT_FOUND"));
    }

    // ---------- cadastro (administrador) ----------

    private org.springframework.test.web.servlet.ResultActions cadastrar(String credencial, Object titulo, Object pontos,
            Object palestraId) throws Exception {
        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("descricao", "");
        corpo.put("pontuacao", pontos);
        corpo.put("palestraId", palestraId);
        var req = post("/quizzes").contentType(JSON).content(json.writeValueAsString(corpo));
        if (credencial != null) req = req.header("Authorization", "Bearer " + credencial);
        return mvc.perform(req);
    }

    @Test
    void somenteAdministradorCadastraQuizEOsDadosSaoValidados() throws Exception {
        cadastrar(null, "Outro quiz", 50, palestra.getId()).andExpect(status().isUnauthorized());
        cadastrar(token, "Outro quiz", 50, palestra.getId()).andExpect(status().isForbidden());
        cadastrar(adminToken, "  ", 50, palestra.getId()).andExpect(status().isBadRequest());
        cadastrar(adminToken, "ab", 50, palestra.getId()).andExpect(status().isBadRequest());
        cadastrar(adminToken, "Outro quiz", 0, palestra.getId()).andExpect(status().isBadRequest());
        cadastrar(adminToken, "Outro quiz", 1001, palestra.getId()).andExpect(status().isBadRequest());
        cadastrar(adminToken, "Outro quiz", null, palestra.getId()).andExpect(status().isBadRequest());
        cadastrar(adminToken, "Outro quiz", 50, null).andExpect(status().isBadRequest());
        cadastrar(adminToken, "Outro quiz", 50, 987654).andExpect(status().isNotFound());
        // a palestra já tem quiz: nem título repetido nem título diferente passam
        cadastrar(adminToken, "QUIZ DA PALESTRA", 50, palestra.getId()).andExpect(status().isConflict());
        cadastrar(adminToken, "Outro quiz", 50, palestra.getId()).andExpect(status().isConflict());
        assertEquals(1, quizzes.count());
        // em outra palestra com perguntas válidas o cadastro funciona
        Palestra outra = palestraComPergunta("Segunda palestra", "Bruno");
        cadastrar(adminToken, "Outro quiz", 50, outra.getId()).andExpect(status().isCreated());
        assertEquals(2, quizzes.count());
    }

    @Test
    void naoPermiteMaisDeUmQuizPorPalestra() throws Exception {
        cadastrar(adminToken, "Segundo quiz", 50, palestra.getId()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value(
                        "Esta palestra já tem um quiz cadastrado. Edite o quiz existente para alterar as perguntas."));
        assertEquals(1, quizzes.count());
    }

    private Palestra palestraComPergunta(String nome, String palestrante) throws Exception {
        Palestra nova = novaPalestra();
        nova.setNome(nome);
        nova.setPalestrante(palestrante);
        nova = palestras.saveAndFlush(nova);
        mvc.perform(post("/palestras/" + nova.getId() + "/add-pergunta").contentType(JSON)
                .content(json.writeValueAsString(Map.of("enunciado", "Pergunta da " + nome, "alternativas", List.of(
                        Map.of("texto", "Certa", "correta", true), Map.of("texto", "Errada", "correta", false))))))
                .andExpect(status().isOk());
        return nova;
    }

    // ---------- edição do quiz (administrador) ----------

    private org.springframework.test.web.servlet.ResultActions editar(String credencial, Object titulo, Object pontos)
            throws Exception {
        Map<String, Object> corpo = new java.util.HashMap<>();
        corpo.put("titulo", titulo);
        corpo.put("descricao", "Nova descrição");
        corpo.put("pontuacao", pontos);
        var req = put("/quizzes/" + quizId).contentType(JSON).content(json.writeValueAsString(corpo));
        if (credencial != null) req = req.header("Authorization", "Bearer " + credencial);
        return mvc.perform(req);
    }

    @Test
    void somenteAdministradorEditaQuizEOsDadosSaoValidados() throws Exception {
        editar(null, "Quiz editado", 80).andExpect(status().isUnauthorized());
        editar(token, "Quiz editado", 80).andExpect(status().isForbidden());
        editar(adminToken, "  ", 80).andExpect(status().isBadRequest());
        editar(adminToken, "ab", 80).andExpect(status().isBadRequest());
        editar(adminToken, "Quiz editado", 0).andExpect(status().isBadRequest());
        editar(adminToken, "Quiz editado", 1001).andExpect(status().isBadRequest());
        editar(adminToken, "Quiz editado", null).andExpect(status().isBadRequest());
        assertEquals("Quiz da palestra", quizzes.findById(quizId).orElseThrow().getTitulo());

        editar(adminToken, "Quiz editado", 80).andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Quiz editado"))
                .andExpect(jsonPath("$.pontuacao").value(80))
                .andExpect(jsonPath("$.totalPerguntas").value(2));
        var salvo = quizzes.findById(quizId).orElseThrow();
        assertEquals("Quiz editado", salvo.getTitulo());
        assertEquals(80, salvo.getPontuacao());
        assertEquals(palestra.getId(), salvo.getPalestra().getId());
    }

    @Test
    void quizInexistenteNaEdicaoRetorna404() throws Exception {
        mvc.perform(put("/quizzes/987654").header("Authorization", "Bearer " + adminToken).contentType(JSON)
                .content(json.writeValueAsString(Map.of("titulo", "Qualquer", "pontuacao", 10))))
                .andExpect(status().isNotFound());
    }

    // ---------- edição, inclusão e exclusão de perguntas refletem no quiz ----------

    @Test
    void editarPerguntaTrocaTextosECorretasEAdicionarAlternativaFunciona() throws Exception {
        Integer id = p1.get("id").asInt();
        List<Map<String, Object>> alternativas = new ArrayList<>();
        // mantém a primeira (agora errada), a segunda vira a correta e entra uma alternativa nova
        alternativas.add(Map.of("id", idAlternativa(p1, 0), "texto", "Brasília (editada)", "correta", false));
        alternativas.add(Map.of("id", idAlternativa(p1, 1), "texto", "Rio de Janeiro", "correta", true));
        alternativas.add(Map.of("texto", "Curitiba", "correta", false));
        mvc.perform(put("/pergunta/" + id).contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "Qual é a capital do Brasil (editada)?", "alternativas", alternativas))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enunciado").value("Qual é a capital do Brasil (editada)?"))
                .andExpect(jsonPath("$.alternativas.length()").value(3));

        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPerguntas").value(2))
                .andExpect(jsonPath("$[0].perguntas[0].enunciado").value("Qual é a capital do Brasil (editada)?"))
                .andExpect(jsonPath("$[0].perguntas[0].alternativas.length()").value(3));
    }

    @Test
    void edicaoDePerguntaInvalidaEhRecusadaSemAlterarNada() throws Exception {
        Integer id = p1.get("id").asInt();
        // só uma alternativa
        mvc.perform(put("/pergunta/" + id).contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "Enunciado válido", "alternativas",
                        List.of(Map.of("id", idAlternativa(p1, 0), "texto", "Brasília", "correta", true))))))
                .andExpect(status().isBadRequest());
        // nenhuma correta
        mvc.perform(put("/pergunta/" + id).contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "Enunciado válido", "alternativas", List.of(
                        Map.of("id", idAlternativa(p1, 0), "texto", "Brasília", "correta", false),
                        Map.of("id", idAlternativa(p1, 1), "texto", "Rio", "correta", false))))))
                .andExpect(status().isBadRequest());
        // todas corretas
        mvc.perform(put("/pergunta/" + id).contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "Enunciado válido", "alternativas", List.of(
                        Map.of("id", idAlternativa(p1, 0), "texto", "Brasília", "correta", true),
                        Map.of("id", idAlternativa(p1, 1), "texto", "Rio", "correta", true))))))
                .andExpect(status().isBadRequest());
        // alternativas repetidas
        mvc.perform(put("/pergunta/" + id).contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "Enunciado válido", "alternativas", List.of(
                        Map.of("id", idAlternativa(p1, 0), "texto", "Brasília", "correta", true),
                        Map.of("id", idAlternativa(p1, 1), "texto", "brasília", "correta", false))))))
                .andExpect(status().isBadRequest());
        // enunciado vazio
        mvc.perform(put("/pergunta/" + id).contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "   "))))
                .andExpect(status().isBadRequest());
        // pergunta inexistente
        mvc.perform(put("/pergunta/987654").contentType(JSON).content(json.writeValueAsString(
                Map.of("enunciado", "Enunciado válido"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void excluirPerguntaTiraEssaPerguntaDoQuiz() throws Exception {
        mvc.perform(put("/pergunta/deletar/" + p2.get("id").asInt())).andExpect(status().isOk());
        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPerguntas").value(1));
    }

    @Test
    void perguntaNovaCadastradaDepoisEntraNoQuiz() throws Exception {
        pergunta("Quanto é 2 + 2?", new String[] { "4", "5", "6" }, 0);
        mvc.perform(get("/quizzes").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].totalPerguntas").value(3));
    }

    @Test
    void naoCadastraQuizParaPalestraSemPerguntasValidas() throws Exception {
        Palestra vazia = novaPalestra();
        vazia.setNome("Sem perguntas");
        vazia.setPalestrante("Carla");
        vazia = palestras.saveAndFlush(vazia);
        cadastrar(adminToken, "Quiz vazio", 50, vazia.getId()).andExpect(status().isBadRequest());
        assertEquals(1, quizzes.count());
    }
}