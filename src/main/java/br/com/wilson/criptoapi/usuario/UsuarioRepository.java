package br.com.wilson.criptoapi.usuario;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * O email e guardado sempre em minusculo pelo servico, entao a busca e por
 * igualdade exata - e usa o indice unico uq_usuarios_email.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);
}
