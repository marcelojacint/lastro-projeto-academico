CREATE TABLE importacao (
    id UUID PRIMARY KEY,
    fornecedor_id BIGINT NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    hash_sha256 VARCHAR(64) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    recebida_em TIMESTAMP WITH TIME ZONE NOT NULL,
    concluida_em TIMESTAMP WITH TIME ZONE,
    status VARCHAR(30) NOT NULL,
    total_itens INTEGER NOT NULL DEFAULT 0,
    itens_validos INTEGER NOT NULL DEFAULT 0,
    itens_pendentes INTEGER NOT NULL DEFAULT 0,
    motivo_falha VARCHAR(30),
    CONSTRAINT fk_importacao_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedor (id),
    CONSTRAINT ck_importacao_nome_arquivo_preenchido CHECK (btrim(nome_arquivo) <> ''),
    CONSTRAINT ck_importacao_hash_sha256 CHECK (hash_sha256 ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_importacao_tamanho_positivo CHECK (tamanho_bytes > 0),
    CONSTRAINT ck_importacao_status CHECK (status IN (
        'RECEBIDA', 'EM_CONFERENCIA', 'CONCLUIDA', 'CONCLUIDA_COM_PENDENCIAS', 'FALHOU', 'DESCARTADA')),
    CONSTRAINT ck_importacao_contadores_nao_negativos
        CHECK (total_itens >= 0 AND itens_validos >= 0 AND itens_pendentes >= 0),
    CONSTRAINT ck_importacao_motivo_falha
        CHECK (motivo_falha IN ('LAYOUT_NAO_RECONHECIDO', 'PDF_ILEGIVEL', 'ERRO_INTERNO')),
    CONSTRAINT ck_importacao_motivo_so_em_falha CHECK (motivo_falha IS NULL OR status = 'FALHOU')
);

CREATE UNIQUE INDEX uk_importacao_fornecedor_hash
    ON importacao (fornecedor_id, hash_sha256)
    WHERE status NOT IN ('DESCARTADA', 'FALHOU');

CREATE INDEX ix_importacao_fornecedor_recebida_em ON importacao (fornecedor_id, recebida_em DESC);
