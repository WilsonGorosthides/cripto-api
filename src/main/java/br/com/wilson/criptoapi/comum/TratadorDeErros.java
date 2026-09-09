package br.com.wilson.criptoapi.comum;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Um formato de erro para tudo o que passa pelo MVC (ADR 0015).
 *
 * ResponseEntityExceptionHandler ja converte as excecoes do proprio Spring
 * (corpo ilegivel, validacao, rota inexistente...) e qualquer
 * ErrorResponseException - as nossas - em ProblemDetail. Esta classe so
 * ajusta duas coisas que o padrao deixa a desejar:
 *
 *   1. validacao: o Spring diz "Invalid request content" e nada mais. Aqui
 *      entra a lista de campos com a mensagem de cada um;
 *   2. title em portugues e instance sempre preenchido.
 *
 * O 401 do filtro de seguranca nao passa por aqui - acontece antes do MVC.
 */
@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {

    private static final Map<Integer, String> TITULOS = Map.of(
            400, "Requisicao invalida",
            401, "Nao autenticado",
            403, "Acesso negado",
            404, "Nao encontrado",
            405, "Metodo nao permitido",
            409, "Conflito",
            415, "Tipo de conteudo nao suportado",
            500, "Erro interno");

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro.getField(),
                        "mensagem", Objects.requireNonNullElse(erro.getDefaultMessage(), "invalido")))
                .toList();

        ProblemDetail corpo = ex.getBody();
        corpo.setDetail(erros.size() + " campo(s) invalido(s)");
        corpo.setProperty("erros", erros);
        return handleExceptionInternal(ex, corpo, headers, status, request);
    }

    /** Ponto unico por onde toda resposta de erro passa antes de sair. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body,
                                                          HttpHeaders headers,
                                                          HttpStatusCode statusCode,
                                                          WebRequest request) {
        if (body instanceof ProblemDetail problema) {
            problema.setTitle(TITULOS.getOrDefault(statusCode.value(), problema.getTitle()));
            if (problema.getInstance() == null && request instanceof ServletWebRequest servlet) {
                problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
            }
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }
}
