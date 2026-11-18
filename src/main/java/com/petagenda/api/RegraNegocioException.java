package com.petagenda.api;

import org.springframework.http.HttpStatus;

/**
 * Erro "esperado" (dado inválido, horário ocupado, sem permissão...).
 * Leva junto o código HTTP e a mensagem que o usuário vai ler na tela.
 * Quando lançado dentro de um método @Transactional, desfaz tudo o que já
 * tinha sido gravado (é uma RuntimeException).
 */
public class RegraNegocioException extends RuntimeException {

    private final HttpStatus status;

    public RegraNegocioException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
