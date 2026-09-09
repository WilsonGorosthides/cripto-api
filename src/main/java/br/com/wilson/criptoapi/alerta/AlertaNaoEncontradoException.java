package br.com.wilson.criptoapi.alerta;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * 404 tanto para "nao existe" quanto para "existe, mas e de outro usuario".
 * Um 403 no segundo caso confirmaria que o id existe - informacao que o
 * outro usuario nao tem por que receber. Formato: ADR 0015.
 */
public class AlertaNaoEncontradoException extends ErrorResponseException {

    public AlertaNaoEncontradoException(Long id) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                        "Alerta " + id + " nao encontrado"),
                null);
    }
}
