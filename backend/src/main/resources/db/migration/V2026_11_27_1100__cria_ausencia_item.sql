CREATE TABLE ausencia_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    importacao_id UUID NOT NULL,
    apresentacao_id BIGINT NOT NULL,
    ultimo_preco_valido NUMERIC(12, 2) NOT NULL,
    registrada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ausencia_item_importacao FOREIGN KEY (importacao_id) REFERENCES importacao (id),
    CONSTRAINT fk_ausencia_item_apresentacao FOREIGN KEY (apresentacao_id) REFERENCES apresentacao_fornecedor (id),
    CONSTRAINT ck_ausencia_item_preco_positivo CHECK (ultimo_preco_valido > 0),
    CONSTRAINT uk_ausencia_item_importacao_apresentacao UNIQUE (importacao_id, apresentacao_id)
);

CREATE INDEX ix_ausencia_item_apresentacao ON ausencia_item (apresentacao_id);
