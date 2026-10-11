package com.capricha.capricha_api.controller;

import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.service.ArmazenamentoArquivoService;
import com.capricha.capricha_api.service.PalestraService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/palestras")
@CrossOrigin(origins = "*")
public class PalestraController {

    private final PalestraService palestraService;
    private final ArmazenamentoArquivoService armazenamentoArquivoService;

    public PalestraController(
            PalestraService palestraService,
            ArmazenamentoArquivoService armazenamentoArquivoService
    ) {
        this.palestraService = palestraService;
        this.armazenamentoArquivoService = armazenamentoArquivoService;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Palestra cadastrar(@RequestBody Palestra palestra) {
        return palestraService.salvar(palestra);
    }

  
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Palestra cadastrarComImagem(
            @RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam("palestrante") String palestrante,
            @RequestParam("data") String data,
            @RequestParam("horario") String horario,
            @RequestParam("evento") String evento,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem
    ) {

        Palestra palestra = new Palestra();

        palestra.setNome(nome);
        palestra.setDescricao(descricao);
        palestra.setPalestrante(palestrante);
        palestra.setData(data);
        palestra.setHorario(horario);
        palestra.setEvento(evento);

 
        if (imagem != null && !imagem.isEmpty()) {
            String nomeArquivo = armazenamentoArquivoService.salvar(imagem);
            palestra.setImagem(nomeArquivo);
        }

        return palestraService.salvar(palestra);
    }

    @GetMapping
    public List<Palestra> listar() {
        return palestraService.listar();
    }
}