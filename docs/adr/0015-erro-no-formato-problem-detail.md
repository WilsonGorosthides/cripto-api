# 0015 — Responder erro no formato Problem Details (RFC 9457)

**Status:** aceita
**Data:** 2026-09-09

## Contexto

A API já devolve erro em quatro situações — 400 (corpo inválido), 401 (login errado), 404
(alerta de outro usuário), 409 (email repetido) — e cada uma sai com um **corpo
diferente**: as exceções anotadas com `@ResponseStatus` caem no `/error` do Spring Boot
(`timestamp`, `status`, `error`, `path`), a validação do Bean Validation sai por outro
caminho, e o filtro de segurança responde 401 **sem corpo**.

Há ainda um erro que não é erro: `GET /api/v1/moedas/XYZ/historico` para uma moeda que não
existe responde **200 com página vazia**. O teste
`moedaInexistenteDevolvePaginaVaziaEmVezDeErro` foi escrito para afirmar esse comportamento
sabidamente errado e quebrar quando ele fosse corrigido — este é o momento.

Um cliente que consome a API precisa de **um** formato de erro para tratar, não quatro.

## Decisão

Todo erro que passa pelo Spring MVC responde `application/problem+json` no formato da
**RFC 9457** (*Problem Details for HTTP APIs*), usando o `ProblemDetail` que o Spring já
tem:

```json
{
  "type": "about:blank",
  "title": "Nao encontrado",
  "status": 404,
  "detail": "Nao ha coleta para a moeda XYZ",
  "instance": "/api/v1/moedas/XYZ/historico"
}
```

Como:

| peça | decisão |
|---|---|
| exceções de domínio (`MoedaNaoEncontrada`, `AlertaNaoEncontrado`, `EmailJaCadastrado`, `CredenciaisInvalidas`) | passam a estender `ErrorResponseException`, que **carrega** um `ProblemDetail`, em vez de `@ResponseStatus` |
| erros do próprio MVC (corpo ilegível, validação, rota inexistente) | um `@RestControllerAdvice` que estende `ResponseEntityExceptionHandler` — o Spring já converte todos para `ProblemDetail` |
| validação (400) | o mesmo advice acrescenta o campo `erros`, lista de `{campo, mensagem}`. O padrão do Spring diz só "Invalid request content", que não ajuda ninguém |
| moeda inexistente no histórico | **404**, decidido no serviço: página vazia dispara uma verificação de existência; se a moeda nunca foi coletada, lança. Página vazia de moeda real (página além do fim) continua 200 |
| 401 do filtro de segurança | **fica como está**, sem corpo. Ele acontece antes do MVC; dar-lhe corpo exigiria um `AuthenticationEntryPoint` próprio. Registrado como inconsistência aceita |

## Alternativas consideradas

### Envelope próprio — `{ "codigo": "MOEDA_NAO_ENCONTRADA", "mensagem": "..." }`

**Vantagem:** total controle do formato e um `codigo` estável que o cliente pode tratar
por `switch`, independente do texto. É o que a maioria das APIs internas faz.

**Desvantagem:** é mais um formato que o cliente precisa aprender, sem documentação além
do README deste projeto. A RFC 9457 é conhecida, tem `type` para o mesmo papel do `codigo`,
e o Spring a implementa — o envelope próprio exigiria escrever à mão o que o framework já
dá. Formato inventado é dívida de documentação perpétua.

### Manter o `/error` padrão do Spring Boot

**Vantagem:** zero código.

**Desvantagem:** o formato (`timestamp`, `status`, `error`, `path`, `message`) é do Boot,
não de norma nenhuma; mudou entre versões (o `message` sumiu por padrão no 2.3) e não
cobre o 401 do filtro nem o 404 da moeda, que continuaria 200. Não resolve o problema.

### 200 com página vazia continua sendo a resposta para moeda inexistente

**Vantagem:** consistente com "página além do fim devolve vazio", uma consulta a menos.

**Desvantagem:** o cliente não distingue "moeda não existe" de "existe e você pediu uma
página longe demais". Um erro de digitação em `/moedas/BTX/historico` passaria em silêncio.
Custa uma consulta a mais **apenas quando a página vem vazia** — o caminho raro.

## Consequências

**Mais fácil:** o cliente trata erro num lugar só. `status` é o HTTP, `detail` é para
mostrar, `instance` é a rota que falhou.

**Mais fácil:** o Spring faz o trabalho pesado — `ResponseEntityExceptionHandler` já mapeia
mais de quinze exceções internas para `ProblemDetail`. O código do projeto se resume a um
override para a validação e às exceções de domínio.

**Mais difícil:** exceções passam a depender do Spring (`ErrorResponseException` é do
`spring-web`). Regra de negócio que lança HTTP é acoplamento — aceito num projeto que é
uma API e não vai virar biblioteca.

**Inconsistência aceita:** o 401 sem token não tem corpo. Está documentado aqui e no README.

## Referências

- RFC 9457, Problem Details for HTTP APIs — <https://www.rfc-editor.org/rfc/rfc9457>
- `ProblemDetail` e `ErrorResponseException` no Spring —
  <https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html>
- Relacionado: [ADR 0004](0004-dto-separado-da-entidade.md),
  [ADR 0008](0008-paginacao-por-offset.md)
