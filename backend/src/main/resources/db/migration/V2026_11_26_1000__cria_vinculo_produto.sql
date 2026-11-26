CREATE TABLE vinculo_produto (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    apresentacao_id BIGINT NOT NULL,
    produto_id BIGINT NOT NULL,
    origem VARCHAR(12) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    confirmado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    desativado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_vinculo_produto_apresentacao FOREIGN KEY (apresentacao_id) REFERENCES apresentacao_fornecedor (id),
    CONSTRAINT fk_vinculo_produto_produto FOREIGN KEY (produto_id) REFERENCES produto (id),
    CONSTRAINT ck_vinculo_produto_origem CHECK (origem IN ('AUTOMATICA', 'MANUAL')),
    CONSTRAINT ck_vinculo_produto_desativado CHECK (ativo OR desativado_em IS NOT NULL)
);

CREATE UNIQUE INDEX uk_vinculo_produto_apresentacao_ativo ON vinculo_produto (apresentacao_id) WHERE ativo;

CREATE INDEX ix_vinculo_produto_produto ON vinculo_produto (produto_id);
