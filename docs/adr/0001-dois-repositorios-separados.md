# 0001 — Manter ingestão e API em repositórios separados

**Status:** aceita
**Data:** 2026-09-03

## Contexto

O `cripto-pipeline` é um projeto Python que coleta preços de criptomoedas de hora em hora
e grava em PostgreSQL. Já está público, concluído e com histórico limpo.

Esta API em Java lê o mesmo banco. As duas partes compartilham dados, mas nenhuma linha de
código. Era preciso decidir se conviviam num repositório só.

## Decisão

Dois repositórios independentes, com referência cruzada explícita nos dois READMEs — o
mesmo diagrama de fluxo e a mesma justificativa de separação em ambos.

## Alternativas consideradas

### Monorepo (`cripto/pipeline` + `cripto/api`)

**Vantagem:** um único link conta a história inteira. Mudanças que atravessam os dois
projetos cabem num commit atômico, e não há risco de as duas partes divergirem sem que
ninguém perceba.

**Desvantagem:** o ganho do monorepo aparece com ferramental compartilhado e mudanças
frequentes que cruzam módulos. Aqui não há nada compartilhado: linguagens diferentes,
builds diferentes, ciclos de vida diferentes. Todo push dispararia CI dos dois. E o
histórico do pipeline, hoje limpo, seria diluído.

### Dois repositórios sem referência entre si

**Vantagem:** independência total, zero acoplamento até na documentação.

**Desvantagem:** quem chega em um deles não descobre o outro. A arquitetura de duas peças
— que é justamente o que há de interessante — fica invisível.

## Consequências

**Mais fácil:** implantar e escalar cada parte no seu ritmo. O pipeline é um job em lote
que roda por segundos e termina; a API é um serviço de vida longa. São unidades de
implantação diferentes, e repositório é unidade de implantação.

**Mais fácil:** o `cripto-pipeline`, que está concluído, permanece intocado.

**Mais difícil:** uma mudança no schema exige commit nos dois repositórios, sem garantia
de atomicidade. É risco real, mitigado pelo ADR 0002: só o pipeline altera o schema, e a
API declara sua dependência dele em `@Immutable` e `ddl-auto=none`.

**Mais difícil:** manter as duas descrições em sincronia é trabalho manual.

## Referências

- Repositório da ingestão: <https://github.com/WilsonGorosthides/cripto-pipeline>
- Relacionado: [ADR 0002](0002-api-nao-e-dona-do-schema.md)
