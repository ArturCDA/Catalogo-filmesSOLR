package br.com.artur.catalogofilmes.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(erro ->
                campos.put(erro.getField(), erro.getDefaultMessage()));

        return ResponseEntity.badRequest().body(corpo(HttpStatus.BAD_REQUEST,
                "Requisição inválida: corrija os campos abaixo", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        log.warn("Corpo da requisição inválido: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(corpo(HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido: verifique o JSON enviado.", detalhes(ex)));
    }

    @ExceptionHandler(ServicoIndisponivelException.class)
    public ResponseEntity<Map<String, Object>> handleServicoIndisponivel(ServicoIndisponivelException ex) {
        log.error("Serviço de busca (Solr) indisponível", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo(
                HttpStatus.SERVICE_UNAVAILABLE,
                "O serviço de busca (Apache Solr) está indisponível no momento. Verifique se o servidor está ativo e tente novamente.",
                detalhes(ex)));
    }

    @ExceptionHandler({SolrServerException.class, SolrException.class, IOException.class})
    public ResponseEntity<Map<String, Object>> handleSolr(Exception ex) {
        log.error("Falha de comunicação com o Apache Solr", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(corpo(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Falha de comunicação com o Apache Solr. Verifique se o servidor está ativo.",
                detalhes(ex)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenerico(Exception ex) {
        log.error("Erro interno não tratado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(corpo(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado.",
                detalhes(ex)));
    }

    private Map<String, Object> corpo(HttpStatus status, String erro, Object detalhes) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("status", status.value());
        corpo.put("erro", erro);
        corpo.put("detalhes", detalhes);
        return corpo;
    }

    private String detalhes(Exception ex) {
        return ex.getMessage() != null
                ? ex.getClass().getSimpleName() + ": " + ex.getMessage()
                : ex.getClass().getSimpleName();
    }
}
