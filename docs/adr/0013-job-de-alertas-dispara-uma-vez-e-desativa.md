# 0013 — O job de alertas dispara uma vez por alerta e o desativa

**Status:** aceita
**Data:** 2026-09-09

## Contexto

Um alerta é "avise quando `BTC` ficar `ACIMA` de `400000`". Quem escreve preço é o
pipeline Python, de hora em hora; a API só lê. Então a API **não sabe quando chega dado
novo** — ela precisa olhar. E precisa decidir três coisas que parecem detalhe e viram
contrato:

1. **Quando** olhar.
2. **O que** comparar: a última coleta, ou qualquer coleta desde a última avaliação.
3. **Quantas vezes** um alerta dispara enquanto a condição continua verdadeira.

O schema já fixou uma parte ([V1](../../src/main/resources/db/migration/V1__tabelas_da_api.sql)):
`alerta_disparos` tem `UNIQUE (alerta_id, coletado_em)`, ou seja, **no máximo um disparo
por alerta por coleta**. O que falta decidir é o comportamento em cima disso.

## Decisão

| pergunta | decisão |
|---|---|
| quando olhar | `@Scheduled` com atraso fixo, padrão **5 minutos** (`cripto.alertas.intervalo`) |
| o que comparar | **só a última coleta**, lida de `vw_cripto_atual` — a mesma view do endpoint de listagem |
| quantas vezes | **uma**. Ao disparar, o alerta vira `ativo = false`. Para ser avisado de novo, o usuário cria outro |

A avaliação é uma consulta só, cruzando `alertas` ativos com a view por `simbolo`, e a
gravação do disparo e a desativação acontecem na mesma transação.

Em teste o agendamento fica **desligado** (`cripto.alertas.agendamento=false`) e o teste
chama o avaliador diretamente. Job rodando em segundo plano durante outros testes é fonte
de resultado que muda de uma execução para outra.

## Alternativas consideradas

### Disparar em toda coleta enquanto a condição valer

**Vantagem:** o histórico de disparos vira registro de "quanto tempo ficou acima". O
`UNIQUE` já garante que não duplica dentro da mesma coleta.

**Desvantagem:** o pipeline coleta 24 vezes por dia. Um alerta de "BTC acima de 100" nunca
deixa de valer e gera 24 linhas por dia, para sempre, até o usuário lembrar de apagar. O
que o usuário quis dizer foi "me avise", não "me lembre a cada hora".

### Disparar só no cruzamento (de fora para dentro da condição)

**Vantagem:** é o comportamento mais fiel ao que "alerta" significa — avisa quando muda,
não quando está. E permite o alerta continuar ativo, disparando de novo se o preço sair e
voltar.

**Desvantagem:** exige guardar o estado anterior de cada alerta ("na última avaliação, a
condição valia?"), ou comparar duas coletas em vez de uma. É mais estado e mais consulta
para um ganho que, com "dispara uma vez e desativa", o usuário obtém criando o alerta de
novo. Se um dia houver interface com botão "reativar", esta alternativa volta.

### Avaliar cada coleta, não só a última

Comparar contra todas as linhas de `precos_cripto` desde a última avaliação.

**Vantagem:** não perde um pico que aconteceu entre duas avaliações. Com intervalo de 5
minutos e coleta de 60, isso não acontece; com intervalo maior que a coleta, aconteceria.

**Desvantagem:** exige lembrar "até onde já avaliei" e varrer a tabela em vez da view. O
[ADR 0006](0006-consulta-na-view-e-nao-em-java.md) decidiu que a última coleta se resolve
na view; esta alternativa reabriria isso. A restrição real — intervalo do job menor que o
intervalo do pipeline — fica documentada na propriedade em vez de codificada.

### Trigger no PostgreSQL, ou `LISTEN/NOTIFY`

**Vantagem:** reage no instante em que a linha entra, sem polling.

**Desvantagem:** trigger em `precos_cripto` é DDL na tabela do pipeline — cruza a
fronteira do [ADR 0002](0002-api-nao-e-dona-do-schema.md). E move regra de negócio para
dentro do banco, onde não há teste de unidade nem `git blame` que a explique.

## Consequências

**Mais fácil:** a regra inteira cabe numa consulta e num laço. É a única regra de negócio
do projeto, e é possível lê-la inteira numa tela.

**Mais fácil:** testável sem tempo. O teste semeia preços e alertas, chama `avaliar()` e
olha `alerta_disparos`. Nenhum `sleep`, nenhum relógio falso.

**Mais difícil:** um pico entre duas avaliações não é visto se o job estiver parado por
mais de uma hora — a API caiu, voltou, e a coleta do meio já não é "a última". Aceito: o
projeto não promete entrega garantida de alerta, e dizer isso é mais honesto que fingir
com fila.

**Mais difícil:** o `simbolo` é a chave de ligação, e o [ADR 0009](0009-adiar-indices-para-os-acessos-da-api.md)
já registrou que `simbolo` pode colidir entre moedas. Com 20 moedas fixas não colide; a
consulta da view custa 4,87 ms medidos e roda a cada 5 minutos — irrelevante hoje, e o
mesmo limiar do ADR 0009 vale para reavaliar.

## Referências

- `@Scheduled` no Spring —
  <https://docs.spring.io/spring-framework/reference/integration/scheduling.html>
- Relacionado: [ADR 0002](0002-api-nao-e-dona-do-schema.md),
  [ADR 0006](0006-consulta-na-view-e-nao-em-java.md),
  [ADR 0011](0011-flyway-apenas-para-as-tabelas-da-api.md)
