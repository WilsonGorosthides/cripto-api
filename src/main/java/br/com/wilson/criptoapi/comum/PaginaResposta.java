package br.com.wilson.criptoapi.comum;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de pagina no CONTRATO da API.
 *
 * Existe porque o Page do Spring Data nao deve ser serializado direto para
 * JSON: ele e uma classe interna do framework, com campos como "pageable",
 * "sort" e "numberOfElements" que expoem detalhe de implementacao e que mudam
 * entre versoes do Spring. Uma atualizacao de dependencia quebraria clientes.
 *
 * Os campos abaixo sao os mesmos de sempre em qualquer API paginada, com nomes
 * escolhidos por nos e estaveis por decisao nossa.
 */
public record PaginaResposta<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalDeItens,
        int totalDePaginas,
        boolean primeira,
        boolean ultima
) {

    /**
     * Converte um Page de entidades num PaginaResposta de DTOs.
     *
     * O parametro conversor evita repetir esta montagem em cada endpoint
     * paginado: quem chama diz apenas como transformar um item.
     */
    public static <E, D> PaginaResposta<D> de(Page<E> page, Function<E, D> conversor) {
        return new PaginaResposta<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
