package br.com.wilson.criptoapi.usuario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 409: o recurso que se tentou criar ja existe. O @ResponseStatus resolve o
 * codigo sem precisar de handler; o formato do corpo e unificado no
 * @RestControllerAdvice, que entra no bloco de tratamento de erro.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EmailJaCadastradoException extends RuntimeException {

    public EmailJaCadastradoException(String email) {
        super("Ja existe usuario com o email " + email);
    }
}
