CREATE TABLE apresentacao_fornecedor (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fornecedor_id BIGINT NOT NULL,
    referencia VARCHAR(50),
    descricao_original VARCHAR(300) NOT NULL,
    descricao_normalizada VARCHAR(300) NOT NULL,
    unidade VARCHAR(10),
    modalidade_preco VARCHAR(10) NOT NULL,
    chave_item VARCHAR(300) GENERATED ALWAYS AS (COALESCE(referencia, descricao_normalizada)) STORED,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_apresentacao_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id),
    CONSTRAINT ck_apresentacao_referencia_preenchida CHECK (btrim(referencia) <> ''),
    CONSTRAINT ck_apresentacao_descricao_preenchida CHECK (btrim(descricao_normalizada) <> ''),
    CONSTRAINT ck_apresentacao_modalidade_preco
        CHECK (modalidade_preco IN ('UNITARIO', 'FARDO', 'ATACADO', 'VAREJO')),
    CONSTRAINT uk_apresentacao_chave
        UNIQUE NULLS NOT DISTINCT (fornecedor_id, chave_item, unidade, modalidade_preco)
);
