package com.petagenda.api;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cabeçalhos HTTP de segurança em todas as respostas (revisão de 21/11/2026).
 *
 * - X-Content-Type-Options: nosniff  -> o navegador não "adivinha" o tipo do arquivo
 * - X-Frame-Options: DENY            -> o painel não pode ser aberto dentro de outro
 *                                       site (proteção contra clickjacking)
 * - Referrer-Policy: same-origin     -> endereços do sistema não vazam para outros sites
 *
 * Não substitui o HTTPS, que depende de onde o sistema for hospedado.
 */
@Component
public class CabecalhosSeguranca extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest pedido, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        resposta.setHeader("X-Content-Type-Options", "nosniff");
        resposta.setHeader("X-Frame-Options", "DENY");
        resposta.setHeader("Referrer-Policy", "same-origin");
        cadeia.doFilter(pedido, resposta);
    }
}
