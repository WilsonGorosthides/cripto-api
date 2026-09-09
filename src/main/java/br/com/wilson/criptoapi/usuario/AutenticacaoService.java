package br.com.wilson.criptoapi.usuario;

import br.com.wilson.criptoapi.seguranca.TokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registro e login (docs/adr/0012).
 *
 * A senha nunca e guardada: o que vai ao banco e o hash BCrypt, e o login
 * compara com PasswordEncoder.matches - nunca re-hasheando e comparando
 * strings, porque o salt de cada hash e diferente.
 */
@Service
public class AutenticacaoService {

    private final UsuarioRepository repositorio;
    private final PasswordEncoder codificador;
    private final TokenService tokens;

    public AutenticacaoService(UsuarioRepository repositorio,
                               PasswordEncoder codificador,
                               TokenService tokens) {
        this.repositorio = repositorio;
        this.codificador = codificador;
        this.tokens = tokens;
    }

    /**
     * A checagem existsByEmail resolve o caso comum com 409 limpo. A unique
     * do banco (uq_usuarios_email) e a garantia real, para o caso de dois
     * registros simultaneos passarem pela checagem ao mesmo tempo.
     */
    @Transactional
    public UsuarioResposta registrar(Credenciais credenciais) {
        String email = normalizar(credenciais.email());
        if (repositorio.existsByEmail(email)) {
            throw new EmailJaCadastradoException(email);
        }
        Usuario usuario = repositorio.save(
                new Usuario(email, codificador.encode(credenciais.senha())));
        return UsuarioResposta.de(usuario);
    }

    @Transactional(readOnly = true)
    public TokenService.Emitido login(Credenciais credenciais) {
        Usuario usuario = repositorio.findByEmail(normalizar(credenciais.email()))
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!codificador.matches(credenciais.senha(), usuario.getSenhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return tokens.emitir(usuario.getId(), usuario.getEmail());
    }

    /** Usuario dono do token apresentado. O id vem do subject do JWT. */
    @Transactional(readOnly = true)
    public UsuarioResposta buscar(Long id) {
        return repositorio.findById(id)
                .map(UsuarioResposta::de)
                .orElseThrow(CredenciaisInvalidasException::new);
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
