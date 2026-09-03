package br.com.wilson.criptoapi.moeda;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acesso a vw_cripto_atual.
 *
 * Repare que esta e uma INTERFACE, sem nenhuma implementacao. O Spring Data
 * gera a classe concreta em tempo de execucao e a registra no conteiner.
 *
 * Os metodos abaixo tambem nao tem corpo: o Spring Data le o NOME do metodo,
 * decompoe em palavras e monta o SQL. "findBySimboloIgnoreCase" vira
 * "SELECT ... WHERE upper(simbolo) = upper(?)".
 *
 * Se o nome nao fizer sentido contra os campos da entidade, a aplicacao FALHA
 * AO SUBIR, com o nome do metodo no erro - nao em tempo de execucao, quando
 * alguem chamar o endpoint.
 */
public interface MoedaAtualRepository extends JpaRepository<MoedaAtual, Long> {

    List<MoedaAtual> findAllByOrderByRankingAsc();

    Optional<MoedaAtual> findBySimboloIgnoreCase(String simbolo);
}
