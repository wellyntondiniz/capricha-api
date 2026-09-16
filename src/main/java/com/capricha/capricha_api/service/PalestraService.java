package com.capricha.capricha_api.service;

import com.capricha.capricha_api.model.Palestra;
import com.capricha.capricha_api.repository.PalestraRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PalestraService {

    private final PalestraRepository palestraRepository;

    public PalestraService(PalestraRepository palestraRepository) {
        this.palestraRepository = palestraRepository;
    }

    public Palestra salvar(Palestra palestra) {
        return palestraRepository.save(palestra);
    }

    public List<Palestra> listar() {
        return palestraRepository.findAll();
    }
}