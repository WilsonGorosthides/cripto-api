package br.com.wilson.criptoapi.moeda;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints de leitura sobre a ultima coleta.
 *
 * O controller nao conhece mais o repositorio: ele so traduz HTTP para chamada
 * de metodo e devolve o resultado. Toda decisao sobre COMO obter o dado ficou
 * do outro lado da fronteira, no MoedaService.
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
}
