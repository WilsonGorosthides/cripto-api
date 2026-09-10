package br.com.wilson.criptoapi.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST /registrar e de POST /login. O mesmo record serve aos dois
 * porque o contrato e o mesmo: email e senha.
 *
 * O maximo de 72 nao e capricho: BCrypt ignora tudo alem do 72o byte, e uma
 * senha "mais longa" seria aceita e silenciosamente truncada.
 */
public record Credenciais(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72) String senha) {
}
