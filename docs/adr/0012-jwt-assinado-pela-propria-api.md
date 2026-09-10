# 0012 — Autenticar com JWT emitido e validado pela própria API

**Status:** aceita
**Data:** 2026-09-09

## Contexto

A API vai ter escrita: alertas pertencem a um usuário, e só ele pode vê-los e apagá-los.
Isso exige saber **quem** está chamando. Até aqui todo endpoint era público e de leitura,
e não havia identidade nenhuma.

Restrições do projeto que pesam na escolha:

- **Uma instância, um banco, sem outro serviço.** Não há gateway, não há segundo
  microsserviço que precise confiar no mesmo token.
- **Sem estado compartilhado** além do PostgreSQL. Não há Redis nem sessão distribuída.
- **Deploy é `docker compose up`** ([ADR 0011](0011-flyway-apenas-para-as-tabelas-da-api.md)).
  Qualquer peça a mais é uma peça a mais no compose.
- A leitura (`GET /api/v1/moedas/**`, `/actuator/health`) **continua pública**. Proteger o
  que já funciona não é o objetivo; o objetivo é proteger o que vai nascer.

## Decisão

**JWT assinado com HMAC (HS256)**, emitido pela própria API no login e validado por ela em
cada requisição. Sem sessão no servidor, sem cookie, sem *refresh token*.

Peças, e de onde vêm:

| peça | escolha | origem |
|---|---|---|
| filtro, regras de rota, `PasswordEncoder` | Spring Security | `spring-boot-starter-security` |
| validar o token em cada requisição | `JwtDecoder` (Nimbus) | `spring-boot-starter-security-oauth2-resource-server` |
| emitir o token no login | `NimbusJwtEncoder`, mesma biblioteca | idem |
| hash da senha | BCrypt, força padrão (10) | Spring Security |
| segredo de assinatura | variável de ambiente `CRIPTO_JWT_SEGREDO`, mínimo 32 bytes | [ADR 0005](0005-segredo-por-variavel-de-ambiente.md) |
| validade | 1 hora, sem renovação | — |

Rotas:

| rota | acesso |
|---|---|
| `GET /api/v1/moedas/**`, `GET /actuator/health` | público |
| `POST /api/v1/auth/registrar`, `POST /api/v1/auth/login` | público |
| tudo o mais | exige `Authorization: Bearer <token>` |

A API se comporta como *resource server* de si mesma: o mesmo módulo que sabe validar um
JWT de terceiros valida o que ela mesma emitiu. Não entra biblioteca de JWT de fora do
Spring.

## Alternativas consideradas

### Sessão com cookie (`HttpSession`)

**Vantagem:** é o padrão do Spring Security sem configurar nada. Revogar é apagar a
sessão; expirar é configuração. Não existe segredo de assinatura para proteger.

**Desvantagem:** estado no servidor. Com uma instância funciona; com duas, precisa de
sessão compartilhada ou *sticky session*. E cookie pede defesa contra CSRF, que numa API
consumida por `curl` e por outro serviço é ruído — o cliente natural de uma API REST manda
cabeçalho, não cookie.

### Biblioteca `jjwt` (io.jsonwebtoken) para emitir e validar

**Vantagem:** API fluente, muito documentada, aparece em quase todo tutorial.

**Desvantagem:** dependência a mais para fazer o que o Spring Security já faz. O módulo
`oauth2-resource-server` traz o Nimbus, o `JwtDecoder` e o filtro que lê o `Bearer` —
usar `jjwt` significaria escrever à mão um filtro que o framework já tem, só para poder
usar a biblioteca escolhida. Prefere-se o que vem do próprio Spring e é mantido junto com
ele.

### Assinatura assimétrica (RS256, par de chaves)

**Vantagem:** quem valida não precisa do segredo — só da chave pública. É o modelo certo
quando outro serviço precisa confiar no token sem poder emitir um.

**Desvantagem:** neste projeto quem emite e quem valida é o mesmo processo. O par de
chaves acrescenta geração, armazenamento e rotação de dois arquivos para separar dois
papéis que aqui não estão separados. Se um segundo serviço aparecer, a troca de HS256
para RS256 é local — muda o `JwtDecoder` e o `JwtEncoder`, não o resto.

### Spring Authorization Server

**Vantagem:** OAuth2 completo — *client credentials*, *authorization code*, *refresh*,
revogação, tudo padronizado.

**Desvantagem:** é um servidor de autorização para um cadastro com dois campos. O compose
ganharia um serviço a mais, e o projeto ganharia um protocolo inteiro para explicar. A
proporção entre o que se ganha e o que se carrega não fecha.

### Argon2 em vez de BCrypt

**Vantagem:** vencedor do *Password Hashing Competition* (2015); resistente a GPU por ser
intensivo em memória.

**Desvantagem:** exige o Bouncy Castle no classpath. BCrypt é o padrão do
`DelegatingPasswordEncoder` do Spring, está no classpath sem nada a mais, e o hash já
carrega o algoritmo no prefixo (`{bcrypt}`) — migrar para Argon2 depois é trocar o
encoder padrão e deixar o Spring re-hashear no próximo login. Nada se perde adiando.

## Consequências

**Mais fácil:** escalar horizontalmente não exige nada — qualquer instância valida qualquer
token, porque todas têm o mesmo segredo.

**Mais fácil:** o filtro, a extração do `Bearer`, o `401` sem token e o `SecurityContext`
já vêm prontos. O código do projeto se resume a: regras de rota, um encoder, um decoder e
o endpoint de login.

**Mais difícil:** **não há revogação.** Um token vazado vale até expirar — por isso a
validade curta e sem renovação. Trocar a senha não invalida tokens já emitidos. A
mitigação padrão (lista de revogados) reintroduz estado no servidor e foi
conscientemente deixada de fora.

**Mais difícil:** o segredo é um ponto único de falha. Quem tiver `CRIPTO_JWT_SEGREDO`
emite token para qualquer usuário. Ele nunca entra em arquivo versionado, e o teste usa um
segredo próprio, fixo, que só existe em `src/test/resources`.

**Aceito conscientemente:** validade de 1 hora sem *refresh* significa relogar a cada hora.
Para uma API sem interface, chamada por script ou por `curl`, é o custo certo. Num
aplicativo com tela seria inaceitável, e a decisão teria sido outra.

## Referências

- Spring Security, JWT como *resource server* —
  <https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html>
- RFC 7519, JSON Web Token — <https://www.rfc-editor.org/rfc/rfc7519>
- BCrypt e o `DelegatingPasswordEncoder` —
  <https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>
- Relacionado: [ADR 0005](0005-segredo-por-variavel-de-ambiente.md),
  [ADR 0011](0011-flyway-apenas-para-as-tabelas-da-api.md)
