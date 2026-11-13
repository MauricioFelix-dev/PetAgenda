package com.petagenda.api;

import java.time.LocalDateTime;

/**
 * Regra central do PetAgenda (motor antichoque).
 *
 * Dois atendimentos entram em conflito quando os intervalos de tempo se sobrepõem.
 * Cada atendimento ocupa o intervalo [inicio, inicio + duracao).
 *
 * Exemplo: Banho + Tosa (75 min) às 10:00 ocupa de 10:00 até 11:15.
 * Um Banho (30 min) às 11:00 conflita. Um Banho às 11:15 não conflita.
 *
 * Esta classe não acessa o banco de dados: é lógica pura e por isso pode ser
 * testada isoladamente (ver RegraConflitoTest).
 */
public final class RegraConflito {

    private RegraConflito() {
    }

    /**
     * Retorna true se os dois intervalos se sobrepõem.
     * Regra: A começa antes de B terminar E A termina depois de B começar.
     * Intervalos que apenas "encostam" (fim de um = início do outro) NÃO conflitam.
     */
    public static boolean haSobreposicao(LocalDateTime inicioA, int duracaoMinA,
                                         LocalDateTime inicioB, int duracaoMinB) {
        LocalDateTime fimA = inicioA.plusMinutes(duracaoMinA);
        LocalDateTime fimB = inicioB.plusMinutes(duracaoMinB);
        return inicioA.isBefore(fimB) && fimA.isAfter(inicioB);
    }
}
