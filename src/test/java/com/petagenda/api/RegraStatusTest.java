package com.petagenda.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Testes do ciclo de vida do agendamento (RegraStatus). Não precisam de banco.
 * Rode com:  mvnw test -Dtest=RegraStatusTest
 */
class RegraStatusTest {

    @Test
    void st01_fluxoNormalPendenteConfirmadoEmAndamentoConcluido() {
        assertTrue(RegraStatus.podeMudar("Pendente", "Confirmado"));
        assertTrue(RegraStatus.podeMudar("Confirmado", "Em andamento"));
        assertTrue(RegraStatus.podeMudar("Em andamento", "Concluído"));
    }

    @Test
    void st02_atendimentoSemConfirmacaoPodeComecarDireto() {
        assertTrue(RegraStatus.podeMudar("Pendente", "Em andamento"));
    }

    @Test
    void st03_cancelarAntesDeConcluir() {
        assertTrue(RegraStatus.podeMudar("Pendente", "Cancelado"));
        assertTrue(RegraStatus.podeMudar("Confirmado", "Cancelado"));
        assertTrue(RegraStatus.podeMudar("Em andamento", "Cancelado"));
    }

    @Test
    void st04_canceladoNaoVoltaAtras() {
        // Era o defeito encontrado na auditoria: reativar pulava o antichoque
        assertFalse(RegraStatus.podeMudar("Cancelado", "Pendente"));
        assertFalse(RegraStatus.podeMudar("Cancelado", "Confirmado"));
        assertFalse(RegraStatus.podeMudar("Cancelado", "Em andamento"));
    }

    @Test
    void st05_concluidoNaoMudaMais() {
        assertFalse(RegraStatus.podeMudar("Concluído", "Cancelado"));
        assertFalse(RegraStatus.podeMudar("Concluído", "Pendente"));
    }

    @Test
    void st06_naoPulaEtapasNemAndaParaTras() {
        assertFalse(RegraStatus.podeMudar("Pendente", "Concluído"));
        assertFalse(RegraStatus.podeMudar("Em andamento", "Pendente"));
        assertFalse(RegraStatus.podeMudar("Confirmado", "Pendente"));
    }

    @Test
    void st07_statusInexistenteEhRecusado() {
        assertFalse(RegraStatus.podeMudar("Pendente", "Pago"));
        assertFalse(RegraStatus.podeMudar(null, "Cancelado"));
    }

    @Test
    void st08_tutorSoCancelaOQueNaoComecou() {
        assertTrue(RegraStatus.tutorPodeCancelar("Pendente"));
        assertTrue(RegraStatus.tutorPodeCancelar("Confirmado"));
        assertFalse(RegraStatus.tutorPodeCancelar("Em andamento"));
        assertFalse(RegraStatus.tutorPodeCancelar("Concluído"));
        assertFalse(RegraStatus.tutorPodeCancelar("Cancelado"));
    }
}
