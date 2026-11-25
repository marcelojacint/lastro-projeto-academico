CREATE TABLE item_importado (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    importacao_id UUID NOT NULL,
    numero_linha INTEGER NOT NULL,
    linha_original VARCHAR(1000) NOT NULL,
    referencia VARCHAR(50),
    descricao VARCHAR(300),
    unidade VARCHAR(10),
    modalidade_preco VARCHAR(10),
    quantidade NUMERIC(12, 3),
    preco NUMERIC(12, 2),
    situacao VARCHAR(25) NOT NULL,
    erros JSONB NOT NULL DEFAULT '[]',
    apresentacao_id BIGINT,
    CONSTRAINT fk_item_importado_importacao FOREIGN KEY (importacao_id) REFERENCES importacao (id),
    CONSTRAINT fk_item_importado_apresentacao
        FOREIGN KEY (apresentacao_id) REFERENCES apresentacao_fornecedor (id),
    CONSTRAINT ck_item_importado_numero_linha_positivo CHECK (numero_linha > 0),
    CONSTRAINT ck_item_importado_modalidade_preco
        CHECK (modalidade_preco IN ('UNITARIO', 'FARDO', 'ATACADO', 'VAREJO')),
    CONSTRAINT ck_item_importado_situacao CHECK (situacao IN ('VALIDO', 'PENDENTE_ASSOCIACAO', 'INVALIDO')),
    CONSTRAINT ck_item_importado_erros_lista CHECK (jsonb_typeof(erros) = 'array'),
    CONSTRAINT uk_item_importado_linha_modalidade
        UNIQUE NULLS NOT DISTINCT (importacao_id, numero_linha, modalidade_preco)
);

CREATE INDEX ix_item_importado_apresentacao ON item_importado (apresentacao_id);
