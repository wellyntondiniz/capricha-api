package com.capricha.capricha_api.controller;

import com.capricha.capricha_api.entidade.Evento;
import com.capricha.capricha_api.entidade.Pergunta;
import com.capricha.capricha_api.entidade.Usuario;
import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.service.ArmazenamentoArquivoService;
import com.capricha.capricha_api.service.PalestraService;
import com.capricha.capricha_api.service.SessaoUsuarioService;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/palestras")
@CrossOrigin
public class PalestraController {

    private final PalestraService palestraService;
    private final ArmazenamentoArquivoService armazenamentoArquivoService;
    private final SessaoUsuarioService sessoes;

    public PalestraController(
            PalestraService palestraService,
            ArmazenamentoArquivoService armazenamentoArquivoService,
            SessaoUsuarioService sessoes
    ) {
        this.palestraService = palestraService;
        this.armazenamentoArquivoService = armazenamentoArquivoService;
        this.sessoes = sessoes;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Palestra cadastrar(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Palestra palestra
    ) {
        exigirAdministrador(authorization);
        return palestraService.salvar(palestra);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Palestra cadastrarComImagem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("nome") String nome,
            @RequestParam(value = "descricao", required = false, defaultValue = "") String descricao,
            @RequestParam("palestrante") String palestrante,
            @RequestParam("data") String data,
            @RequestParam("horario") String horario,
            @RequestParam("evento") Integer eventoId,
            @RequestParam(value = "dataTermino", required = false) String dataTermino,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem
    ) {
        exigirAdministrador(authorization);

        Palestra palestra = montar(nome, descricao, palestrante, data, horario, eventoId, dataTermino);

        String nomeArquivo = armazenamentoArquivoService.salvar(imagem);
        try {
            palestra.setImagem(nomeArquivo);
            return palestraService.salvar(palestra);
        } catch (RuntimeException e) {
            // Não deixa a imagem órfã se a validação falhar.
            armazenamentoArquivoService.remover(nomeArquivo);
            throw e;
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Palestra atualizar(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("nome") String nome,
            @RequestParam(value = "descricao", required = false, defaultValue = "") String descricao,
            @RequestParam("palestrante") String palestrante,
            @RequestParam("data") String data,
            @RequestParam("horario") String horario,
            @RequestParam("evento") Integer eventoId,
            @RequestParam(value = "dataTermino", required = false) String dataTermino,
            @RequestParam(value = "removerImagem", required = false, defaultValue = "false") boolean removerImagem,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem
    ) {
        exigirAdministrador(authorization);

        Palestra dados = montar(nome, descricao, palestrante, data, horario, eventoId, dataTermino);

        String nomeArquivo = armazenamentoArquivoService.salvar(imagem);
        try {
            return palestraService.atualizar(id, dados, nomeArquivo, removerImagem);
        } catch (RuntimeException e) {
            armazenamentoArquivoService.remover(nomeArquivo);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        exigirAdministrador(authorization);
        palestraService.excluir(id);
    }

    @GetMapping
    public List<Palestra> listar() {
        return palestraService.listar();
    }

    @GetMapping("/{id}")
    public Palestra buscar(@PathVariable Long id) {
        return palestraService.buscar(id);
    }

    @PostMapping("/{id}/add-pergunta")
    public Pergunta adicionarPergunta(@PathVariable Long id, @RequestBody Pergunta pergunta) {
        return palestraService.adicionarPergunta(id, pergunta);
    }

    @GetMapping("/{id}/perguntas")
    public List<Pergunta> listarPerguntas(@PathVariable Long id) {
        return palestraService.listarPerguntas(id);
    }

    private Palestra montar(
            String nome,
            String descricao,
            String palestrante,
            String data,
            String horario,
            Integer eventoId,
            String dataTermino
    ) {
        Palestra palestra = new Palestra();

        palestra.setNome(nome);
        palestra.setDescricao(descricao);
        palestra.setPalestrante(palestrante);
        palestra.setData(data);
        palestra.setHorario(horario);

        if (dataTermino != null && !dataTermino.isBlank()) {
            try {
                palestra.setDataTermino(LocalDateTime.parse(dataTermino.trim()));
            } catch (DateTimeParseException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data e horário de término inválidos.");
            }
        }

        Evento evento = new Evento();
        evento.setId(eventoId);
        palestra.setEvento(evento);

        return palestra;
    }

    private void exigirAdministrador(String authorization) {
        Usuario usuario = sessoes.autenticar(authorization);
        String perfil = usuario.getPerfil();
        if (!"ADMIN".equalsIgnoreCase(perfil) && !"ADMINISTRADOR".equalsIgnoreCase(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente administradores podem gerenciar palestras.");
        }
    }
}