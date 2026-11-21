package com.petagenda.api;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Transforma os erros em respostas JSON no formato {"erro": "mensagem"},
 * que é o formato que as telas já sabem mostrar.
 */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraDeNegocio(RegraNegocioException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> banco(DataAccessException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("erro", "Falha ao gravar no banco de dados. Nada foi salvo."));
    }

    /**
     * Pedido malformado: JSON quebrado, parâmetro faltando ou com tipo errado
     * (ex.: servico=abc). Revisão de 21/11/2026: antes a resposta vinha no formato
     * padrão do Spring, sem o campo "erro", e a tela mostrava só "Erro 400".
     */
    @ExceptionHandler({ HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, Object>> pedidoMalformado(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "Dados do pedido inválidos ou incompletos."));
    }
}
