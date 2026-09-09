# cripto-api — API REST sobre a série histórica de preços

[![ci](https://github.com/WilsonGorosthides/cripto-api/actions/workflows/ci.yml/badge.svg)](https://github.com/WilsonGorosthides/cripto-api/actions/workflows/ci.yml)

API em Spring Boot que serve os dados coletados pelo
[cripto-pipeline](https://github.com/WilsonGorosthides/cripto-pipeline), e deixa um usuário
cadastrar alertas de preço que um job avalia contra a coleta mais recente.

```
CoinGecko ──▶ cripto-pipeline (Python) ──▶ PostgreSQL ──▶ cripto-api (Java) ──▶ HTTP
                  de hora em hora              │             sob demanda
                                               └──▶ job de alertas, a cada 5 min
```

Dois repositórios porque são duas unidades de implantação: o pipeline é um job em lote que
roda por segundos e termina; a API é um serviço de vida longa. Cada um sobe, escala e falha
sem o outro.

## O que existe

| rota | acesso | devolve |
|---|---|---|
| `GET /api/v1/moedas` | público | as 20 moedas da coleta mais recente, por ranking |
| `GET /api/v1/moedas/{simbolo}/historico?page=&size=` | público | série temporal paginada, mais recente primeiro; `size` até 100 |
| `POST /api/v1/auth/registrar` | público | `201` com `{id, email}` |
| `POST /api/v1/auth/login` | público | `{token, expiraEm}` — JWT válido por 1 h |
| `GET /api/v1/auth/eu` | Bearer | quem é o dono do token |
| `POST /api/v1/alertas` | Bearer | cria `{simbolo, condicao: ACIMA\|ABAIXO, valor}` |
| `GET /api/v1/alertas` | Bearer | só os alertas do usuário do token |
| `DELETE /api/v1/alertas/{id}` | Bearer | `204`; alerta de outro usuário responde `404` |
| `GET /api/v1/alertas/{id}/disparos` | Bearer | quando e com que preço o alerta disparou |
| `GET /actuator/health`, `/liveness`, `/readiness` | público | `{"status":"UP"}` |

**Job de alertas:** a cada 5 minutos compara os alertas ativos com a última coleta de cada
moeda. Quando a condição vale, grava em `alerta_disparos` e **desativa o alerta** — dispara
uma vez. Para ser avisado de novo, cria-se outro ([ADR 0013](docs/adr/0013-job-de-alertas-dispara-uma-vez-e-desativa.md)).

**Erros** saem como `application/problem+json` (RFC 9457): `status`, `title`, `detail`,
`instance`; a validação acrescenta `erros: [{campo, mensagem}]`. Exceção: o `401` sem token
vem do filtro de segurança, antes do MVC, e **não tem corpo**
([ADR 0015](docs/adr/0015-erro-no-formato-problem-detail.md)).

## O que não existe, de propósito

| fora | onde está a razão |
|---|---|
| URL pública, HTTPS, VPS | [ADR 0014](docs/adr/0014-publicar-imagem-e-ci-em-vez-de-vps.md) — projeto congelado; link morto é pior que link nenhum |
| Kubernetes | tirado do escopo pelo autor; entra num projeto com mais de um serviço |
| refresh token, revogação de JWT | [ADR 0012](docs/adr/0012-jwt-assinado-pela-propria-api.md) — validade curta no lugar de estado no servidor |
| notificação do alerta (e-mail, push) | o disparo fica em `alerta_disparos`; entregar é outro sistema |
| índices para as consultas da API | [ADR 0009](docs/adr/0009-adiar-indices-para-os-acessos-da-api.md) — medido, com limiar de reavaliação |
| documentação OpenAPI/Swagger | não chegou a entrar; as rotas estão na tabela acima |

## Como rodar

### Com Docker — o sistema inteiro numa máquina limpa

```bash
cp .env.exemplo .env          # e troque os dois valores
docker compose up -d --wait
curl localhost:8080/api/v1/moedas
```

Sobe um PostgreSQL 17 já com o schema do pipeline e **60 linhas de dados reais** (3 coletas
de 09/09/2026, em [docker/dados-exemplo.sql](docker/dados-exemplo.sql)), e a API em cima.
Medido nesta máquina: build da imagem 117 s na primeira vez, imagem final 372 MB,
`up --wait` 22 s até os dois healthchecks ficarem verdes.

A imagem `ghcr.io/wilsongorosthides/cripto-api` é publicada pelo CI a cada merge em `main`.
Se ela ainda não existir no registro, o compose constrói localmente a partir do
[Dockerfile](Dockerfile) — o resultado é o mesmo.

### Sem Docker — contra o PostgreSQL do pipeline

Requer Java 17+ e um PostgreSQL com o schema do `cripto-pipeline` aplicado. Maven não
precisa ser instalado; o wrapper cuida disso.

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "senha-do-postgres"
$env:CRIPTO_JWT_SEGREDO = "um-segredo-aleatorio-com-pelo-menos-32-bytes"
.\mvnw spring-boot:run
```

Na primeira subida o Flyway cria `usuarios`, `alertas` e `alerta_disparos` nesse banco. Ele
**não toca** em `precos_cripto` nem na view ([ADR 0011](docs/adr/0011-flyway-apenas-para-as-tabelas-da-api.md)).

### Um ciclo completo

```bash
curl -X POST localhost:8080/api/v1/auth/registrar -H 'Content-Type: application/json' \
     -d '{"email":"ana@exemplo.com","senha":"senha-forte-123"}'
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"ana@exemplo.com","senha":"senha-forte-123"}' | jq -r .token)
curl -X POST localhost:8080/api/v1/alertas -H "Authorization: Bearer $TOKEN" \
     -H 'Content-Type: application/json' -d '{"simbolo":"BTC","condicao":"ACIMA","valor":1}'
# ate 5 min depois:
curl localhost:8080/api/v1/alertas/1/disparos -H "Authorization: Bearer $TOKEN"
```

## Configuração

Tudo em [application.properties](src/main/resources/application.properties), **menos os
segredos**, que vêm do ambiente:

| variável | vira | obrigatória |
|---|---|---|
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | sim |
| `CRIPTO_JWT_SEGREDO` | `cripto.jwt.segredo` — mínimo 32 bytes; a API recusa subir com menos | sim |
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` (padrão `localhost:5432/cripto`) | não |
| `CRIPTO_ALERTAS_INTERVALO` | `cripto.alertas.intervalo` (padrão `PT5M`) | não |

Qualquer propriedade do arquivo pode ser sobrescrita assim: maiúscula vira minúscula,
sublinhado vira ponto. Não há biblioteca nem código envolvido.

## Testes

```bash
./mvnw test        # exige Docker rodando
```

**45 testes, todos contra PostgreSQL 17 real** em container, via Testcontainers — nenhum H2.
O container nasce com o schema do pipeline aplicado (um espelho do `schema.sql`, cuja
fidelidade o CI confere a cada push) e o Flyway migra as tabelas da API em cima. O
`ddl-auto=validate` do perfil de teste quebra o build se uma entidade divergir do schema.

| classe | cobre |
|---|---|
| `LeituraDeMoedasTest` | view, paginação, moeda inexistente → exceção |
| `AutenticacaoTest` | registro, hash BCrypt no banco, login, token aceito de volta |
| `AlertasTest` | contrato HTTP: escopo por usuário, 404 para alerta alheio |
| `AvaliacaoDeAlertasTest` | a regra do job: só a última coleta conta, dispara uma vez, desativa |
| `TratamentoDeErroTest` | formato `problem+json` para exceções nossas e do Spring |
| `MigracoesTest` | Flyway criou as tabelas certas e não tocou nas do pipeline |
| `DockerDisponivelTest` | Docker e Testcontainers respondem a partir do build |

Os testes não usam relógio: o agendamento fica desligado no perfil de teste e o job é
chamado diretamente.

## Decisões de implementação

Cada decisão está em **[docs/adr/](docs/adr/)** com contexto, alternativas descartadas —
com vantagem e desvantagem reais —, consequências e referências. Quinze registros; os
sete primeiros retroativos e declarados como tais, os demais escritos no momento da
decisão, antes de implementar.

As que mais restringem o que vem depois:

- **A API não é dona do schema de `precos_cripto`** ([0002](docs/adr/0002-api-nao-e-dona-do-schema.md)).
  `ddl-auto=none`, `@Immutable`, nenhuma migration a menciona, nenhum FK aponta para ela.
- **`BigDecimal`, nunca `double`, para dinheiro** ([0003](docs/adr/0003-bigdecimal-para-valor-monetario.md)).
- **Paginação por offset** ([0008](docs/adr/0008-paginacao-por-offset.md)) — medida contra keyset
  (0,68 ms vs 0,14 ms na quinta página) e escolhida mesmo assim, com o limiar para rever.
- **Flyway só para as tabelas da API** ([0011](docs/adr/0011-flyway-apenas-para-as-tabelas-da-api.md)) —
  e o `baseline-version=0` sem o qual a `V1` nunca rodaria.
- **JWT HS256 emitido e validado pela própria API** ([0012](docs/adr/0012-jwt-assinado-pela-propria-api.md)).
- **Job dispara uma vez e desativa** ([0013](docs/adr/0013-job-de-alertas-dispara-uma-vez-e-desativa.md)).
- **Imagem + compose + CI no lugar de VPS** ([0014](docs/adr/0014-publicar-imagem-e-ci-em-vez-de-vps.md)).

## Stack

Java 17 · Spring Boot 4.1.1 · Spring Security 7.1.1 · Spring Data JPA / Hibernate 7.4.5 ·
Flyway 12.4.0 · PostgreSQL 17 · Testcontainers 2.0.5 · Maven (wrapper) · Docker · GitHub Actions
