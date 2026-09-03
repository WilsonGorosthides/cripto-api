package br.com.wilson.criptoapi.moeda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Primeira versao, DE PROPOSITO INCOMPLETA.
 *
 * Devolve a entidade direto, sem camada de servico e sem DTO. Serve para ver o
 * JSON que sai daqui - e o que sai vai motivar o proximo bloco.
 */
@RestController
@RequestMapping("/api/v1/moedas")
public class MoedaController {

    private final MoedaAtualRepository repository;

    // Sem @Autowired: quando a classe tem UM construtor, o Spring o usa
    // automaticamente para injetar. O campo e final, entao ninguem troca a
    // dependencia depois de construida.
    public MoedaController(MoedaAtualRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MoedaAtual> listar() {
        return repository.findAllByOrderByRankingAsc();
    }
}
