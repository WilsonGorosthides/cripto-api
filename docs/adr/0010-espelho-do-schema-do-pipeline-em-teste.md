# 0010 — Espelhar o schema do pipeline num script de teste

**Status:** aceita
**Data:** 2026-09-09

## Contexto

O Testcontainers sobe um PostgreSQL limpo a cada execução de teste. Isso foi verificado
empiricamente em `DockerDisponivelTest`:

```
[infra] tabelas no schema public : 0
```

A API mapeia duas estruturas que ela **não possui**: a tabela `precos_cripto` e a view
`vw_cripto_atual`, ambas criadas pelo `cripto-pipeline`. O [ADR 0002](0002-api-nao-e-dona-do-schema.md)
fixa `ddl-auto=none`, então o Hibernate não as cria — corretamente.

Consequência: qualquer teste de integração que suba o contexto do Spring e toque num
repositório falha com `relation "precos_cripto" does not exist`.

A DDL dessas estruturas já existe em **três lugares**, todos no repositório do pipeline:

| onde | conteúdo |
|---|---|
| `coleta_cripto.py` → `DDL_POSTGRES` | tabela + índice |
| `coleta_cripto.py` → `DDL_SQLITE` | o mesmo, em dialeto SQLite |
| `schema.sql` | tabela + índice + **a view** |

A view existe apenas no `schema.sql`; o pipeline sozinho não a cria.

## Decisão

Manter um **espelho** do schema do pipeline em `src/test/resources/schema-pipeline.sql`,
aplicado ao container pelo `withInitScript` do Testcontainers.

Três restrições fazem parte da decisão:

1. **Fora de `db/migration/`.** O arquivo não é migration e não pode ser confundido com
   uma. O caminho `src/test/resources/` já declara o escopo.
2. **Cabeçalho declarando que é cópia**, com o link para a fonte da verdade.
3. **Cópia fiel, sem dados de exemplo.** Linha a linha igual ao original, para que a
   comparação automatizada (abaixo) produza diff limpo. Dados de teste são inseridos pelos
   próprios testes.

## Alternativas consideradas

### Não testar contra as tabelas do pipeline

Testes de integração cobririam apenas `usuarios`, `alertas` e `alerta_disparos` — tabelas
da própria API, sob Flyway. Os endpoints de leitura ficariam com teste de unidade e
repositório dublado.

**Vantagem:** zero duplicação de DDL, e a fronteira do ADR 0002 permanece intocada. É a
opção mais limpa conceitualmente.

**Desvantagem que a derrubou:** o job de avaliação de alertas — a única regra de negócio
real deste projeto — **lê `precos_cripto`** para decidir se dispara. Sem preços no banco de
teste, essa regra não tem como ser testada. A opção não custa "alguns testes de mapeamento";
custa o teste mais importante do projeto.

### Buscar o `schema.sql` do pipeline durante o build

Submódulo git, ou download do arquivo cru do GitHub no ciclo de build.

**Vantagem:** resolve o problema certo — uma fonte da verdade, sem cópia que envelhece.

**Desvantagem:** o build da API passa a exigir rede e a depender do outro repositório.
`mvnw test` sem conexão falha. E recoloca entre os dois projetos o acoplamento que o
[ADR 0001](0001-dois-repositorios-separados.md) separou deliberadamente.

O benefício desta alternativa é recuperado pela mitigação 2, sem o custo.

### Flyway gerenciar tudo, inclusive `precos_cripto`

Uma trilha única de migrations, sem distinção entre tabela da API e tabela do pipeline.

**Vantagem:** uniforme e simples de explicar.

**Desvantagem:** contradiz o ADR 0002 frontalmente. Em produção haveria o Flyway e o
`CREATE TABLE IF NOT EXISTS` do Python achando ambos que mandam na mesma tabela. Uma
migration que alterasse `precos_cripto` quebraria o pipeline na coleta seguinte — o cenário
exato que o ADR 0002 existe para impedir.

## Consequências

**Mais fácil:** testes de integração passam a ser possíveis, contra PostgreSQL real, com a
view e o `DISTINCT ON` funcionando. Isso destrava o teste da regra de alertas.

**Mais fácil:** o build permanece autocontido — sem rede, sem submódulo, sem dependência
do outro repositório.

**Mais difícil:** é a **quarta** cópia da mesma DDL. O problema de duplicação que o pipeline
já tinha internamente agora atravessa a fronteira entre repositórios.

**Risco assumido, sem atenuante:** se o pipeline ganhar uma coluna e o espelho não for
atualizado, **os testes continuam passando** contra um schema desatualizado. A falha aparece
só em execução real. Nenhuma das mitigações abaixo elimina isso por completo.

## Mitigações

### 1. `ddl-auto=validate` no perfil de teste

O Hibernate compara as entidades com o schema do container ao subir o contexto e **falha**
se divergirem. Pega a deriva entre o espelho e as classes Java — a mais provável no dia a
dia, porque as duas são tocadas pela mesma pessoa em momentos diferentes.

Não pega a deriva entre o espelho e o pipeline: se a coluna nova não estiver em nenhum dos
dois lados da API, a validação passa.

### 2. Comparação automatizada no CI — planejada, ainda não implementada

Um passo de CI que baixa o `schema.sql` do `cripto-pipeline` e o compara com o espelho,
falhando o build se divergirem.

É esta mitigação que resolve o risco de fato, e é o motivo de a decisão exigir cópia
**fiel**: qualquer edição cosmética no espelho produz diff falso.

Entra junto com o pipeline de CI. Até lá, o controle é humano — e vale dizer que controle
humano falha.

## Referências

- `withInitScript` no Testcontainers —
  <https://java.testcontainers.org/modules/databases/jdbc/>
- Validação de schema pelo Hibernate —
  <https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html#schema-generation>
- Fonte espelhada —
  <https://github.com/WilsonGorosthides/cripto-pipeline/blob/main/schema.sql>
- Relacionado: [ADR 0001](0001-dois-repositorios-separados.md),
  [ADR 0002](0002-api-nao-e-dona-do-schema.md)
