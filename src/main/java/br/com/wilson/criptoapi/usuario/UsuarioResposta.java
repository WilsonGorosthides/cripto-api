package br.com.wilson.criptoapi.usuario;

/**
 * O que a API devolve sobre um usuario. Sem senhaHash, de proposito:
 * o record so tem o que pode sair (docs/adr/0004).
 */
public record UsuarioResposta(Long id, String email) {

    public static UsuarioResposta de(Usuario usuario) {
        return new UsuarioResposta(usuario.getId(), usuario.getEmail());
    }
}
