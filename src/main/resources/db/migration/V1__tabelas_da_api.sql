-- Tabelas que a API possui. Nada aqui toca em precos_cripto nem em
-- vw_cripto_atual: essas pertencem ao cripto-pipeline (docs/adr/0002, 0011).

CREATE TABLE usuarios (
    id          BIGSERIAL PRIMARY KEY,
    email       TEXT        NOT NULL,
    senha_hash  TEXT        NOT NULL,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuarios_email UNIQUE (email)
);

-- Sem chave estrangeira para precos_cripto, de proposito: a ligacao com a
-- moeda e logica, por simbolo. Um FK amarraria este schema a uma tabela que
-- a API nao controla.
CREATE TABLE alertas (
    id          BIGSERIAL PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    simbolo     TEXT        NOT NULL,
    condicao    TEXT        NOT NULL,
    valor       NUMERIC(24, 8) NOT NULL,
    ativo       BOOLEAN     NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_alertas_condicao CHECK (condicao IN ('ACIMA', 'ABAIXO'))
);

CREATE INDEX idx_alertas_usuario ON alertas (usuario_id);
CREATE INDEX idx_alertas_ativos  ON alertas (simbolo) WHERE ativo;

-- Um disparo por alerta por coleta. A chave unica e o que torna o job
-- idempotente: rodar duas vezes sobre a mesma coleta nao duplica disparo.
CREATE TABLE alerta_disparos (
    id            BIGSERIAL PRIMARY KEY,
    alerta_id     BIGINT      NOT NULL REFERENCES alertas (id) ON DELETE CASCADE,
    preco         NUMERIC(24, 8) NOT NULL,
    coletado_em   TIMESTAMPTZ NOT NULL,
    disparado_em  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_disparo_por_coleta UNIQUE (alerta_id, coletado_em)
);
