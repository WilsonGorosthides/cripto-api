package br.com.wilson.criptoapi.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/** 409: o recurso que se tentou criar ja existe. Formato: ADR 0015. */
public class EmailJaCadastradoException extends ErrorResponseException {

    public EmailJaCadastradoException(String email) {
        super(HttpStatus.CONFLICT,
                ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                        "Ja existe usuario com o email " + email),
                null);
    }
}
