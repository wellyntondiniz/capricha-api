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
        boolean duplicataExata = palestraRepository.existsByNomeAndDataAndHorarioAndPalestrante(
            palestra.getNome(),
            palestra.getData(),
            palestra.getHorario(),
            palestra.getPalestrante()
        );

        if (duplicataExata) {
            throw new IllegalStateException("Esta palestra já está cadastrada.");
        }

        boolean conflitoAgenda = palestraRepository.existsByPalestranteAndDataAndHorario(
            palestra.getPalestrante(),
            palestra.getData(),
            palestra.getHorario()
        );

        if (conflitoAgenda) {
            throw new IllegalStateException(
                "Este palestrante já tem uma palestra cadastrada nesta data e horário."
            );
        }

        return palestraRepository.save(palestra);
    }

    public List<Palestra> listar() {
        return palestraRepository.findAll();
    }
}