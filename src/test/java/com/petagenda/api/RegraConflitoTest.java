package com.petagenda.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Testes unitários da regra antichoque.
 * Não precisam de banco de dados: rode com  ./mvnw test -Dtest=RegraConflitoTest
 */
class RegraConflitoTest {

    private static LocalDateTime hora(int h, int m) {
        return LocalDateTime.of(2026, 11, 20, h, m);
    }

    @Test
    void ct01_mesmoHorarioConflita() {
        assertTrue(RegraConflito.haSobreposicao(hora(10, 0), 30, hora(10, 0), 30));
    }

    @Test
    void ct02_sobreposicaoParcialConflita() {
        // Banho + Tosa 10:00-11:15 x Banho 11:00-11:30
        assertTrue(RegraConflito.haSobreposicao(hora(10, 0), 75, hora(11, 0), 30));
    }

    @Test
    void ct03_atendimentoDentroDeOutroConflita() {
        // Banho + Tosa 10:00-11:15 x Escovação 10:30-10:45
        assertTrue(RegraConflito.haSobreposicao(hora(10, 0), 75, hora(10, 30), 15));
    }

    @Test
    void ct04_horariosQueEncostamNaoConflitam() {
        // Banho 10:00-10:30 x Banho 10:30-11:00
        assertFalse(RegraConflito.haSobreposicao(hora(10, 0), 30, hora(10, 30), 30));
    }

    @Test
    void ct05_horariosSeparadosNaoConflitam() {
        assertFalse(RegraConflito.haSobreposicao(hora(8, 0), 30, hora(14, 0), 45));
    }

    @Test
    void ct06_ordemDosArgumentosNaoImporta() {
        assertTrue(RegraConflito.haSobreposicao(hora(11, 0), 30, hora(10, 0), 75));
    }
}
