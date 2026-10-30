package com.capricha.capricha_api.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.WebApplicationContext;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.*;
import com.capricha.capricha_api.service.Senhas;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@ActiveProfiles("test")
class QuizIntegracaoTest {
    private static final String AUTH = "Authorization";

    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired SessaoUsuarioRepository sessoes;
    @Autowired PalestraRepository palestras;
    @Autowired PresencaPalestraRepository presencasPalestra;
    @Autowired ParticipacaoRepository participacoes;
    @Autowired EventoRepository eventos;
    @Autowired QuizRepository quizzes;
    @Autowired RespostaQuizRepository respostas;
    @Autowired PontuacaoRankingRepository pontuacoes;
    @Autowired PlatformTransactionManager transacoes;
    private final ObjectMapper json = new ObjectMapper();
    private MockMvc mvc;
    private String tokenAdmin, tokenAna, tokenBruno;
    private Palestra palestra;

    @BeforeEach
    void preparar() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        limpar();
        tokenAdmin = login(usuario("admin@teste.com", "Admin", "ADMIN"));
        tokenAna = login(usuario("ana@teste.com", "Ana", "PARTICIPANTE"));
        tokenBruno = login(usuario("bruno@teste.com", "Bruno", "PARTICIPANTE"));
        Palestra nova = new Palestra();
        nova.setNome("Fundamentos de Java");
        nova.setPalestrante("Fulano");
        nova.setData("01/01/2030");
        nova.setHorario("10:00");
        palestra = palestras.saveAndFlush(nova);
    }

    // O quiz referencia a palestra; sem limpar, os outros testes não conseguiriam apagar as palestras
    @AfterEach
    void limpar() {
        respostas.deleteAll();
        pontuacoes.deleteAll();
        quizzes.deleteAll();
        presencasPalestra.deleteAll();
        palestras.deleteAll();
        sessoes.deleteAll();
        participacoes.deleteAll();
        eventos.deleteAll();
        usuarios.deleteAll();
    }

    @Test
    void participanteRespondeUmaVezComAsPerguntasDaPalestra() throws Exception {
        // Pergunta com duas corretas (A e C), cadastrada pelo módulo de perguntas da palestra
        adicionarPergunta("Quais são linguagens da JVM?", Map.of("Java", true, "Python", false, "Kotlin", true));
        adicionarPergunta("Java é compilado para?", Map.of("Bytecode", true, "Assembly", false));

        JsonNode quiz = json.readTree(cadastrarQuiz(tokenAdmin).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        assertEquals(2, quiz.get("perguntas").size());
        // O gabarito não pode ir para o participante
        assertFalse(quiz.toString().contains("correta"));

        long quizId = quiz.get("id").asLong();
        List<Map<String, Object>> certas = respostasCorretas(quizId);

        responder(tokenAna, quizId, certas).andExpect(status().isOk())
                .andExpect(jsonPath("$.acertos").value(2))
                .andExpect(jsonPath("$.pontosObtidos").value(100));

        // Segunda tentativa do mesmo usuário é recusada
        responder(tokenAna, quizId, certas).andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Você já respondeu este quiz."));

        // Outro usuário responde normalmente; marcar só uma das duas corretas não conta o acerto
        List<Map<String, Object>> parcial = new ArrayList<>();
        for (Map<String, Object> resposta : certas) {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) resposta.get("alternativaIds");
            parcial.add(ids.size() > 1 ? Map.of("perguntaId", resposta.get("perguntaId"), "alternativaIds", ids.subList(0, 1)) : resposta);
        }
        responder(tokenBruno, quizId, parcial).andExpect(status().isOk())
                .andExpect(jsonPath("$.acertos").value(1))
                .andExpect(jsonPath("$.pontosObtidos").value(50));

        mvc.perform(get("/quizzes/respostas").header(AUTH, "Bearer " + tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quizId").value(quizId));
        assertEquals(2, respostas.count());
    }

    @Test
    void responderExigeSessao() throws Exception {
        adicionarPergunta("Java é compilado para?", Map.of("Bytecode", true, "Assembly", false));
        long quizId = json.readTree(cadastrarQuiz(tokenAdmin).andReturn().getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/quizzes/" + quizId + "/respostas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"respostas\":[]}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/quizzes/respostas")).andExpect(status().isUnauthorized());
    }

    @Test
    void somenteAdministradorCadastraQuiz() throws Exception {
        adicionarPergunta("Java é compilado para?", Map.of("Bytecode", true, "Assembly", false));
        cadastrarQuiz(tokenAna).andExpect(status().isForbidden());
        assertEquals(0, quizzes.count());
    }

    @Test
    void palestraSemPerguntasNaoGeraQuiz() throws Exception {
        cadastrarQuiz(tokenAdmin).andExpect(status().isBadRequest());
    }

    private Usuario usuario(String email, String nome, String perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setSenha(Senhas.proteger("Senha@123"));
        u.setPerfil(perfil);
        return usuarios.saveAndFlush(u);
    }

    private String login(Usuario u) throws Exception {
        String body = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", u.getEmail(), "senha", "Senha@123"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asString();
    }

    private void adicionarPergunta(String enunciado, Map<String, Boolean> alternativas) throws Exception {
        List<Map<String, Object>> lista = new ArrayList<>();
        new TreeMap<>(alternativas).forEach((texto, correta) -> lista.add(Map.of("texto", texto, "correta", correta)));
        mvc.perform(post("/palestras/" + palestra.getId() + "/add-pergunta").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("enunciado", enunciado, "ativo", true, "alternativas", lista))))
                .andExpect(status().isOk());
    }

    private ResultActions cadastrarQuiz(String token) throws Exception {
        return mvc.perform(post("/quizzes").header(AUTH, "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("titulo", "Quiz de Java", "pontuacao", 100,
                        "palestraId", palestra.getId()))));
    }

    // Monta o gabarito a partir do banco, já que a API do quiz não o expõe
    private List<Map<String, Object>> respostasCorretas(long quizId) {
        return new TransactionTemplate(transacoes).execute(status -> {
            List<Map<String, Object>> lista = new ArrayList<>();
            for (var pergunta : quizzes.findById(quizId).orElseThrow().getPalestra().getPerguntas()) {
                List<Integer> corretas = pergunta.getAlternativas().stream()
                        .filter(a -> a.getCorreta()).map(a -> a.getId()).toList();
                lista.add(Map.of("perguntaId", pergunta.getId(), "alternativaIds", corretas));
            }
            return lista;
        });
    }

    private ResultActions responder(String token, long quizId, List<Map<String, Object>> respostasQuiz) throws Exception {
        return mvc.perform(post("/quizzes/" + quizId + "/respostas").header(AUTH, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("respostas", respostasQuiz))));
    }
}
