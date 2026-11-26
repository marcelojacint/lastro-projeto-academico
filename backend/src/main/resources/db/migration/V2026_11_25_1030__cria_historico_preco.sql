CREATE TABLE historico_preco (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    apresentacao_id BIGINT NOT NULL,
    importacao_id UUID NOT NULL,
    preco NUMERIC(12, 2) NOT NULL,
    registrado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_historico_preco_apresentacao FOREIGN KEY (apresentacao_id) REFERENCES apresentacao_fornecedor (id),
    CONSTRAINT fk_historico_preco_importacao FOREIGN KEY (importacao_id) REFERENCES importacao (id),
    CONSTRAINT ck_historico_preco_positivo CHECK (preco > 0),
    CONSTRAINT uk_historico_preco_apresentacao_importacao UNIQUE (apresentacao_id, importacao_id)
);

CREATE INDEX ix_historico_preco_apresentacao_registrado_em
    ON historico_preco (apresentacao_id, registrado_em DESC, id DESC);
