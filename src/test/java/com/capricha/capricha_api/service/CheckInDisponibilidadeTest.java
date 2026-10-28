package com.capricha.capricha_api.service;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CheckInDisponibilidadeTest {
    @Test
    void inicioInclusivoETerminoExclusivoParaEventosEPalestras() {
        var inicio = LocalDateTime.of(2026, 10, 17, 10, 0);
        var fim = inicio.plusHours(1);
        assertFalse(CheckInDisponibilidade.disponivel(true, inicio, fim, inicio.minusNanos(1)));
        assertTrue(CheckInDisponibilidade.disponivel(true, inicio, fim, inicio));
        assertTrue(CheckInDisponibilidade.disponivel(true, inicio, fim, fim.minusNanos(1)));
        assertFalse(CheckInDisponibilidade.disponivel(true, inicio, fim, fim));
        assertFalse(CheckInDisponibilidade.disponivel(true, inicio, fim, fim.plusNanos(1)));
    }

    @Test
    void cadastroAntigoSemTerminoNaoGanhaDuracaoInventada() {
        var inicio = LocalDateTime.of(2026, 10, 17, 10, 0);
        assertTrue(CheckInDisponibilidade.disponivel(true, inicio, null, inicio.plusDays(1)));
        assertFalse(CheckInDisponibilidade.disponivel(false, inicio, null, inicio));
        assertFalse(CheckInDisponibilidade.disponivel(true, null, null, inicio));
    }
}
