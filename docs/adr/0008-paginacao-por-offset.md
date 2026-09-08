# 0008 — Paginar o histórico por offset, não por keyset

**Status:** aceita
**Data:** 2026-09-08

## Contexto

O endpoint `GET /api/v1/moedas/{simbolo}/historico` devolve a série temporal de uma moeda.
No momento desta decisão eram 57 pontos por moeda, crescendo cerca de 24 por dia enquanto a
máquina estiver ligada. Projeção: ~720 em um mês, ~8.760 em um ano de coleta contínua.

Paginação não é opcional numa API de série temporal — o cliente é quem define quanto pede, e
sem limite ele pode pedir tudo. A escolha da *forma* de paginar, porém, é contrato público:
mudá-la depois quebra todos os clientes.

Foram medidos os dois planos de execução no banco real, com 1.120 linhas:

```
OFFSET 40:   Sort ... rows=50    quicksort         0,681 ms
KEYSET:      Sort ... rows=10    top-N heapsort    0,144 ms
```

Com `OFFSET 40`, o PostgreSQL ordena 50 linhas para devolver 10 — precisa produzir as 40 que
vai descartar. Com keyset, ordena 10.

## Decisão

Paginação por **offset**, com `?page=` e `?size=`, usando `Pageable` e `Page` do Spring Data.
Teto de 100 itens por página em `spring.data.web.pageable.max-page-size`.

A resposta é embrulhada num record próprio, `PaginaResposta<T>`, e não no `Page` do Spring.

## Alternativas consideradas

### Paginação por keyset (cursor)

`?desde=2026-09-04T00:00:00Z&size=20`, traduzido para
`WHERE coletado_em < ? ORDER BY coletado_em DESC LIMIT ?`.

**Vantagem:** custo constante por página, independente de quão fundo o cliente navega —
medido em 0,144 ms contra 0,681 ms na quinta página. E é **estável sob escrita**: como o
pipeline insere linhas novas no topo da série a cada hora, com offset um cliente paginando
devagar pode ver o mesmo registro duas vezes, ou pular um, porque a numeração das páginas se
desloca embaixo dele. Com cursor isso não acontece.

**Desvantagem:** não permite salto arbitrário — não existe "ir para a página 7", porque o
cliente só consegue avançar a partir de onde parou. Perde-se o `totalDePaginas`, que exigiria
um `COUNT(*)` separado. E o cursor precisa ser sobre uma coluna única: `coletado_em` é único
por moeda nesta tabela, mas isso é uma propriedade do pipeline, não uma garantia do schema —
duas coletas no mesmo instante quebrariam a paginação silenciosamente.

### Sem paginação, com limite fixo

Devolver sempre os últimos N pontos, sem parâmetro.

**Vantagem:** contrato mínimo, uma consulta, nada para o cliente entender.

**Desvantagem:** torna o histórico completo inalcançável pela API. O projeto existe para
acumular série temporal; um endpoint que só mostra a ponta dela contradiz o propósito.

## Consequências

**Mais fácil:** o cliente salta para qualquer página, e sabe quantas existem. Isso importa
para quem monta interface com numeração de páginas.

**Mais fácil:** vem pronto do Spring Data. Zero código de paginação, e o comportamento é o
que qualquer desenvolvedor Java espera.

**Mais difícil:** o custo por página cresce conforme o cliente navega, porque o banco produz
e descarta as linhas puladas. Em 8.760 pontos, a última página exigirá ordenar ~8.760 linhas
para devolver 20.

**Mais difícil:** cada requisição dispara **duas** consultas — a página e um `COUNT(*)` para
o total. É o preço do `totalDePaginas`.

**Aceito conscientemente:** a instabilidade sob escrita. Como o pipeline insere no topo e a
ordenação é decrescente, uma inserção entre duas requisições desloca todas as páginas em uma
posição. Na prática o cliente típico lê as primeiras páginas, onde o deslocamento é de um
item por hora.

## Quando revisitar

Esta decisão deve ser refeita se:

- o histórico ultrapassar **50 mil pontos por moeda**, ou
- a resposta da última página passar de **200 ms**, ou
- aparecer cliente que percorre a série inteira sequencialmente — o caso em que a
  instabilidade sob escrita deixa de ser teórica.

A migração seria aditiva, não destrutiva: aceitar um parâmetro `?desde=` opcional ao lado de
`?page=`, mantendo os dois durante um período de transição.

## Nota sobre o envelope da resposta

O `Page` do Spring Data **não é serializado direto para JSON**. Ele é classe interna do
framework, com campos como `pageable`, `sort` e `numberOfElements` que expõem detalhe de
implementação e mudam entre versões — uma atualização de dependência quebraria clientes. O
record `PaginaResposta<T>` tem sete campos escolhidos por nós e estáveis por decisão nossa.

## Referências

- Paginação no Spring Data —
  <https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html>
- `LIMIT` e `OFFSET` no PostgreSQL —
  <https://www.postgresql.org/docs/17/queries-limit.html>
- Relacionado: [ADR 0009](0009-adiar-indices-para-os-acessos-da-api.md)
