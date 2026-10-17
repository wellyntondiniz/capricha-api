package com.capricha.capricha_api.service;

import java.time.LocalDateTime;

/** Mesma fronteira de horário para ambos os recursos: início incluso, término exclusivo. */
final class CheckInDisponibilidade {
    private CheckInDisponibilidade() {}

    static boolean disponivel(boolean ativo, LocalDateTime inicio, LocalDateTime termino, LocalDateTime agora) {
        return ativo && inicio != null && !agora.isBefore(inicio) && (termino == null || agora.isBefore(termino));
    }
}
