# 0003 — Usar `BigDecimal` para valor monetário, nunca ponto flutuante

**Status:** aceita
**Data:** 2026-09-03

## Contexto

As colunas `preco`, `market_cap`, `volume_24h`, `variacao_24h_pct` e `oferta_circulante`
são `NUMERIC` no PostgreSQL, com escalas entre 2 e 8 casas decimais. A escolha de
`NUMERIC` em vez de `DOUBLE PRECISION` já havia sido tomada no `cripto-pipeline`, pelo
mesmo motivo que se repete aqui.

Era preciso decidir o tipo Java correspondente. `Double` é mais simples, ocupa menos
memória e é o tipo que a maior parte dos exemplos usa.

## Decisão

`BigDecimal` para toda coluna `NUMERIC`. Nenhum campo monetário ou de quantidade contável
usa `double` ou `float`, em nenhuma camada — entidade, DTO ou cálculo interno.

## Alternativas consideradas

### `Double`

**Vantagem:** tipo primitivo encaixotado, mais leve em memória e muito mais rápido em
aritmética. Sintaxe direta: `a + b` em vez de `a.add(b)`. Em cálculo numérico intensivo, a
diferença de desempenho é de ordens de grandeza.

**Desvantagem:** ponto flutuante binário (IEEE 754) não representa exatamente a maioria
das frações decimais. Medido nesta base de dados, com o preço real da moeda RAIN:

```
Uma leitura isolada        double: 0.08556               BigDecimal: 0.08556000
Somado 10 vezes            double: 0.8555999999999998    BigDecimal: 0.855600
Igual a 0.85560 ?          double: false                 BigDecimal: true
Oferta 709196851273.4626   double: 7.091968512734626E11  BigDecimal: 709196851273.4626
```

Três defeitos distintos: o erro se acumula em somas; a comparação por igualdade falha; e a
serialização de valores grandes produz notação científica no JSON, que muitos clientes não
esperam de um campo numérico.

O agravante é que uma leitura isolada **parece correta**. O defeito passa em verificação
manual e aparece no relatório de fim de mês.

### Inteiro em centavos (`long`)

**Vantagem:** exato, rápido e é a solução clássica em sistemas financeiros. Sem objeto,
sem alocação.

**Desvantagem:** `preco` tem 8 casas decimais, não 2 — criptomoeda não é moeda de centavo.
Seria preciso fixar um fator de escala arbitrário (10^8) e aplicá-lo em toda entrada e
saída, à mão, sem que nada no tipo lembrasse disso. Um esquecimento gera erro de fator
100.000.000, silencioso.

## Consequências

**Mais fácil:** a precisão do banco atravessa Java e chega ao JSON sem perda. Verificado na
resposta real: `"preco":412659.00000000` e `"ofertaCirculante":20079084.0000`.

**Mais fácil:** comparações e somas são exatas, o que importará quando houver alertas de
preço comparando valor coletado com limite configurado pelo usuário.

**Mais difícil:** aritmética fica verbosa — `a.add(b)`, `a.compareTo(b)`. E há uma
armadilha própria: `equals` de `BigDecimal` considera escala, então `2.0` e `2.00` são
diferentes. Comparação de valor exige `compareTo() == 0`.

**Mais difícil:** custo de memória e velocidade. Irrelevante numa resposta de 20 linhas;
seria decisivo em cálculo numérico de larga escala.

## Referências

- David Goldberg, *What Every Computer Scientist Should Know About Floating-Point
  Arithmetic*, ACM Computing Surveys (1991) —
  <https://docs.oracle.com/cd/E19957-01/806-3568/ncg_goldberg.html>
- Tipos numéricos do PostgreSQL —
  <https://www.postgresql.org/docs/17/datatype-numeric.html>
