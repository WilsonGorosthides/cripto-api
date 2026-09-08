package br.com.wilson.criptoapi.moeda;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acesso a serie historica.
 *
 * O parametro Pageable e o retorno Page vem do Spring Data. Ao receber um
 * Pageable, ele executa DUAS consultas: uma com LIMIT/OFFSET para trazer a
 * pagina, e um COUNT(*) para saber o total. O count e o que permite responder
 * "quantas paginas existem" - e e por isso que a paginacao por offset custa
 * mais do que parece.
 *
 * Ordenacao decrescente fixa no nome do metodo, e nao no Pageable: o mais
 * recente primeiro e parte do contrato deste endpoint, nao preferencia do
 * cliente. Deixar no Pageable permitiria ao cliente pedir ordem crescente e
 * receber os precos mais antigos da serie inteira, que nao e o caso de uso.
 */
public interface PrecoRepository extends JpaRepository<Preco, Long> {

    Page<Preco> findBySimboloIgnoreCaseOrderByColetadoEmDesc(String simbolo, Pageable pageable);
}
