package com.capricha.capricha_api.controller;

import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.service.PalestraService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/palestras")
@CrossOrigin(origins = "*")
public class PalestraController {

    private final PalestraService palestraService;

    public PalestraController(PalestraService palestraService) {
        this.palestraService = palestraService;
    }

    @PostMapping
    public Palestra cadastrar(@RequestBody Palestra palestra) {
        return palestraService.salvar(palestra);
    }

    @GetMapping
    public List<Palestra> listar() {
        return palestraService.listar();
    }
}