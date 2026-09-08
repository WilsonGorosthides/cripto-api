package br.com.wilson.criptoapi.moeda;

import br.com.wilson.criptoapi.comum.PaginaResposta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de leitura sobre moedas.
 *
 * O controller nao conhece o repositorio: ele so traduz HTTP para chamada de
 * metodo e devolve o resultado.
 */
@RestController
@RequestMapping("/api/v1/moedas")
public class MoedaController {

    private final MoedaService service;

    public MoedaController(MoedaService service) {
        this.service = service;
    }

    @GetMapping
    public List<MoedaResposta> listar() {
        return service.listarAtuais();
    }

    /**
     * GET /api/v1/moedas/{simbolo}/historico?page=0&size=20
     *
     * O @PageableDefault define o que acontece quando o cliente nao pede nada.
     * Sem ele o padrao do Spring seria 20 itens, o que ja serve - mas deixar
     * explicito documenta a decisao e protege contra mudanca de padrao numa
     * atualizacao do framework.
     *
     * O teto de itens por pagina fica em application.properties
     * (spring.data.web.pageable.max-page-size). Sem teto, ?size=1000000 traria
     * a serie inteira e anularia o proposito da paginacao.
     */
    @GetMapping("/{simbolo}/historico")
    public PaginaResposta<PontoHistorico> historico(
            @PathVariable String simbolo,
            @PageableDefault(size = 20, sort = "coletadoEm", direction = Sort.Direction.DESC)
            Pageable paginacao) {
        return service.buscarHistorico(simbolo, paginacao);
    }
}
