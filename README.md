# cripto-api — API REST sobre a série histórica de preços

API em Spring Boot que serve os dados coletados pelo
[cripto-pipeline](https://github.com/WilsonGorosthides/cripto-pipeline).

```
CoinGecko ──▶ cripto-pipeline (Python) ──▶ PostgreSQL ──▶ cripto-api (Java) ──▶ HTTP
                  de hora em hora                            sob demanda
```

Dois repositórios porque são duas unidades de implantação. O pipeline é um job em lote
que roda por segundos e termina; a API é um serviço de vida longa. Cada um sobe, escala e
falha sem o outro.

## Estado atual

O que existe e responde:

| endpoint | devolve |
|---|---|
| `GET /api/v1/moedas` | as 20 moedas da coleta mais recente, ordenadas por ranking |
| `GET /actuator/health` | `{"status":"UP"}`, com os grupos `liveness` e `readiness` |

O que ainda **não** existe: DTOs separando entidade de contrato, camada de serviço,
endpoint de histórico, autenticação, testes automatizados e containerização.

## Como rodar

Requer Java 17+ e um PostgreSQL com o schema do `cripto-pipeline` aplicado.
Maven não precisa ser instalado — o wrapper cuida disso.

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "sua-senha"
.\mvnw spring-boot:run
```

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/moedas
```

## Configuração

Tudo em [application.properties](src/main/resources/application.properties), **menos a
senha**. Ela vem da variável de ambiente `SPRING_DATASOURCE_PASSWORD`, que o Spring mapeia
sozinho para `spring.datasource.password` — não há biblioteca nem código envolvido.

Qualquer propriedade do arquivo pode ser sobrescrita pelo ambiente da mesma forma:
maiúscula vira minúscula, sublinhado vira ponto.

## Decisões de implementação

- **A API não é dona do schema.** A tabela `precos_cripto` pertence ao pipeline Python.
  Isso está escrito em dois lugares: `ddl-auto=none`, que proíbe o Hibernate de emitir
  qualquer DDL, e `@Immutable` na entidade, que bloqueia escrita. A alternativa comum,
  `ddl-auto=update`, não daria erro — o Hibernate ajustaria a estrutura por conta própria
  e o `INSERT` do pipeline quebraria na hora seguinte, com a depuração acontecendo do
  lado errado.

- **`BigDecimal`, nunca `double`, para valor monetário.** Ponto flutuante binário parece
  correto numa leitura isolada e falha em soma, em comparação por igualdade, e serializa
  número grande em notação científica. É o `NUMERIC(24,8)` do banco preservado até o JSON.

- **A listagem mapeia a view, não a tabela.** O `DISTINCT ON` da `vw_cripto_atual` resolve
  "a linha mais recente por moeda" no banco, apoiado num índice composto que já existe.
  Em Java, seria trazer o histórico inteiro para a memória e filtrar depois.

- **`open-in-view=false`.** A sessão do JPA fecha ao sair da camada de serviço, e não ao
  fim da resposta HTTP. Com o padrão `true`, consultas preguiçosas disparam durante a
  serialização do JSON, segurando conexão do pool e produzindo erro longe da causa.

- **Injeção por construtor, sem `@Autowired`.** Permite campo `final`, garante que o objeto
  nasça com todas as dependências, e torna a classe instanciável em teste sem subir o
  contexto do Spring.

- **Pacotes por assunto, não por camada.** `moeda/` contém entidade, repositório e
  controller juntos. O layout `controller/` + `repository/` + `entity/` parece mais
  organizado e obriga a abrir três pastas distantes para mexer numa coisa só.

## Stack

Java 17 · Spring Boot 4.1.1 · Spring Data JPA · PostgreSQL · Maven (wrapper)
