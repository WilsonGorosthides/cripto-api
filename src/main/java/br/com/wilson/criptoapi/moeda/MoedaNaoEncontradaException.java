package br.com.wilson.criptoapi.moeda;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * 404 para simbolo que o pipeline nunca coletou (ADR 0015).
 *
 * Estende ErrorResponseException em vez de usar @ResponseStatus: a excecao
 * ja carrega o ProblemDetail, e o ResponseEntityExceptionHandler a
 * transforma em application/problem+json sem handler dedicado.
 */
public class MoedaNaoEncontradaException extends ErrorResponseException {

    public MoedaNaoEncontradaException(String simbolo) {
        super(HttpStatus.NOT_FOUND,
                ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                        "Nao ha coleta para a moeda " + simbolo),
                null);
    }
}
