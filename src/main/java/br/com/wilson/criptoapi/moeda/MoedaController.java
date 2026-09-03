package br.com.wilson.criptoapi.moeda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de leitura sobre a ultima coleta.
 *
 * O metodo devolve List<MoedaResposta>, nao List<MoedaAtual>: a assinatura
 * do metodo E a documentacao do contrato. Quem le a classe sabe, sem abrir
 * mais nada, o que sai daqui.
 */
@RestController
@RequestMapping("/api/v1/moedas")
public class MoedaController {

    private final MoedaAtualRepository repository;

    public MoedaController(MoedaAtualRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<MoedaResposta> listar() {
        return repository.findAllByOrderByRankingAsc()
                .stream()
                .map(MoedaResposta::de)
                .toList();
    }
}
