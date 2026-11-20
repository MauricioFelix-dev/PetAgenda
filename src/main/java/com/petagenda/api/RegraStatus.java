package com.petagenda.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida de um agendamento (métodos confirmar(), cancelar() e finalizar()
 * do diagrama de classes).
 *
 *   Pendente ──► Confirmado ──► Em andamento ──► Concluído
 *      │             │               │
 *      └─────────────┴───────────────┴──► Cancelado
 *
 * Concluído e Cancelado são estados finais: não voltam atrás.
 * Correção (auditoria 17/11/2026): antes a API aceitava qualquer troca, e um
 * agendamento Cancelado podia voltar a Pendente sem passar pelo antichoque,
 * criando duas reservas no mesmo horário.
 *
 * Lógica pura, sem banco de dados: testada em RegraStatusTest.
 */
public final class RegraStatus {

    public static final String PENDENTE = "Pendente";
    public static final String CONFIRMADO = "Confirmado";
    public static final String EM_ANDAMENTO = "Em andamento";
    public static final String CONCLUIDO = "Concluído";
    public static final String CANCELADO = "Cancelado";

    /** Na ordem em que aparecem no ciclo de vida. */
    public static final List<String> TODOS = List.of(PENDENTE, CONFIRMADO, EM_ANDAMENTO, CONCLUIDO, CANCELADO);

    private static final Map<String, Set<String>> PERMITIDAS = Map.of(
            PENDENTE, Set.of(CONFIRMADO, EM_ANDAMENTO, CANCELADO),
            CONFIRMADO, Set.of(EM_ANDAMENTO, CANCELADO),
            EM_ANDAMENTO, Set.of(CONCLUIDO, CANCELADO),
            CONCLUIDO, Set.of(),
            CANCELADO, Set.of());

    private RegraStatus() {
    }

    public static boolean existe(String status) {
        return status != null && TODOS.contains(status);
    }

    /** true se o agendamento pode passar do status atual para o novo. */
    public static boolean podeMudar(String atual, String novo) {
        return existe(atual) && existe(novo) && PERMITIDAS.get(atual).contains(novo);
    }

    /** O tutor só cancela pela internet o que ainda não começou. */
    public static boolean tutorPodeCancelar(String status) {
        return PENDENTE.equals(status) || CONFIRMADO.equals(status);
    }
}
