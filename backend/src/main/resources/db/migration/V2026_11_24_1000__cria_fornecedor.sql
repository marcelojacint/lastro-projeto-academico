CREATE TABLE fornecedor (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome VARCHAR(200) NOT NULL,
    cnpj VARCHAR(14),
    contato VARCHAR(200),
    layout_pdf VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_fornecedor_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_fornecedor_nome_preenchido CHECK (btrim(nome) <> ''),
    CONSTRAINT ck_fornecedor_cnpj_digitos CHECK (cnpj ~ '^[0-9]{14}$'),
    CONSTRAINT ck_fornecedor_layout_pdf CHECK (layout_pdf IN ('TABELA_REFERENCIA', 'ATACADO_VAREJO'))
);
