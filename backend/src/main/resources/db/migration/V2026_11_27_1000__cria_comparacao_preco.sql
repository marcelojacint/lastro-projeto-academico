CREATE TABLE comparacao_preco (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    importacao_id UUID NOT NULL,
    item_importado_id BIGINT NOT NULL,
    preco_anterior NUMERIC(12, 2),
    preco_atual NUMERIC(12, 2),
    diferenca NUMERIC(12, 2),
    variacao_percentual NUMERIC(17, 2),
    classificacao VARCHAR(15) NOT NULL,
    comparada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comparacao_preco_importacao FOREIGN KEY (importacao_id) REFERENCES importacao (id),
    CONSTRAINT fk_comparacao_preco_item FOREIGN KEY (item_importado_id) REFERENCES item_importado (id),
    CONSTRAINT ck_comparacao_preco_classificacao
        CHECK (classificacao IN ('AUMENTOU', 'DIMINUIU', 'SEM_ALTERACAO', 'NOVO', 'PENDENTE')),
    CONSTRAINT ck_comparacao_preco_novo_sem_anterior
        CHECK (classificacao <> 'NOVO' OR (preco_anterior IS NULL AND diferenca IS NULL AND variacao_percentual IS NULL)),
    CONSTRAINT ck_comparacao_preco_pendente_sem_diferenca
        CHECK (classificacao <> 'PENDENTE' OR (diferenca IS NULL AND variacao_percentual IS NULL)),
    CONSTRAINT ck_comparacao_preco_diferenca_quando_comparado
        CHECK (classificacao NOT IN ('AUMENTOU', 'DIMINUIU', 'SEM_ALTERACAO')
            OR (preco_anterior IS NOT NULL AND preco_atual > 0 AND diferenca IS NOT NULL)),
    CONSTRAINT uk_comparacao_preco_item UNIQUE (item_importado_id)
);

CREATE INDEX ix_comparacao_preco_importacao ON comparacao_preco (importacao_id);
