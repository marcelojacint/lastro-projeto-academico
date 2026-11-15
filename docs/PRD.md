# Lastro — PRD do Projeto Integrador DevOps

Nov 15, 2026 · @Marcelo

## Visão geral

O projeto se chama **Lastro**: um sistema web que transforma as tabelas de preço em PDF dos fornecedores em histórico rastreável e compara automaticamente cada novo preço com o último preço válido conhecido.

O nome vem da ideia de lastro como base que sustenta uma decisão: cada preço tem origem (o PDF, a importação, a data) e pode ser conferido depois. É curto, em português, funciona como nome de repositório (`lastro`) e de pacote Java (`br.edu.uniesp.lastro`).

| Opção | Ideia | Repositório |
| --- | --- | --- |
| **Lastro** (escolhido) | Histórico que sustenta a decisão de compra | `lastro` |
| Cotaria | Lugar onde as cotações ficam guardadas | `cotaria` |
| Variar | Foco na variação de preço entre importações | `variar` |

**Problema.** Uma loja de produtos naturais recebe tabelas de preço em PDF de vários fornecedores. A conferência manual não mostra aumentos, reduções, valores antigos nem quais itens correspondem a quais produtos da loja.

**Objetivo do produto.** Importar o PDF, conferir os itens extraídos, associar cada item a um produto interno, gravar o histórico sem sobrescrever nada e exibir a classificação: aumentou, diminuiu, sem alteração, novo ou pendente.

**Objetivo da disciplina.** Entregar isso com práticas verificáveis: Git flow com Pull Requests revisados, cards no GitHub Projects, testes automatizados das regras críticas, pipeline no GitHub Actions, Docker Compose e logs de importação.

## Escopo

O MVP cobre os requisitos RF01 a RF10 do documento do professor e é o que vai para a demonstração final. A evolução só começa com o MVP na `main`.

**MVP (obrigatório)**

| ID | Requisito | Épico |
| --- | --- | --- |
| RF01 | Cadastrar, consultar e editar produtos (código, nome, categoria, unidade, preço de compra, preço de venda, estoque atual, estoque mínimo, status) | Produtos |
| RF02 | Cadastrar e consultar fornecedores | Fornecedores |
| RF03 | Registrar vínculo entre item/apresentação do fornecedor e produto interno | Associação |
| RF04 | Enviar PDF associado a fornecedor e registrar data, arquivo, quantidade de itens e status | Importação |
| RF05 | Extrair referência, descrição, unidade/embalagem, quantidade e preço; exibir para conferência | Importação |
| RF06 | Sinalizar itens inválidos ou não reconhecidos e mantê-los pendentes | Importação |
| RF07 | Comparar cada preço válido com o último preço válido do mesmo fornecedor e apresentação | Precificação |
| RF08 | Classificar: aumentou, diminuiu, sem alteração, novo ou pendente | Precificação |
| RF09 | Consultar histórico por produto, fornecedor e importação | Histórico |
| RF10 | Exibir preço anterior, atual, diferença absoluta e variação percentual com status textual | Precificação |

**Evolução (depois do MVP, por ordem de prioridade)**

1. Sinalizar reposição quando estoque atual for menor ou igual ao estoque mínimo.
2. Sugerir revisão de preço de venda a partir de uma margem configurada, sem alterar o preço cadastrado.
3. Gráfico de evolução de preço por item e indicadores no painel.
4. Suporte a novos layouts de PDF e melhoria no reconhecimento de produtos.

**Fora de escopo:** autenticação com múltiplos perfis, PDF escaneado (OCR), microsserviços, deploy em nuvem paga. Um usuário administrador fixo basta para a demonstração.

## Regras de negócio

Toda comparação usa a **apresentação comercial** como chave, não o código isolado: fornecedor + identificador do item (referência ou descrição normalizada) + unidade + modalidade de preço (KG, FARDO, ATACADO, VAREJO).

| Código | Regra | Decisão de implementação |
| --- | --- | --- |
| RN01 | Comparar com o último preço válido da mesma apresentação | Consulta em `historico_preco` ordenada por `registrado_em desc`, limite 1. Não olha só a importação anterior |
| RN02 | Primeira ocorrência = NOVO | Sem preço-base: diferença e percentual ficam nulos |
| RN03 | Preço zero ou ausente = PENDENTE | Item fica `INVALIDO`, não entra no histórico e não gera comparação válida |
| RN04 | Código repetido não identifica sozinho | Chave única da apresentação inclui unidade e modalidade |
| RN05 | Descrição divergente só associa quando seguro | Automático apenas com vínculo já confirmado ou correspondência exata; o resto vai para associação manual, que fica salva |
| RN06 | Item ausente em PDF posterior não é descontinuado | Grava `ausencia_item` e mantém o último preço válido |
| RN07 | Reimportação do mesmo arquivo | SHA-256 do arquivo + fornecedor; se já existe importação não descartada, responde 409 com o id da importação original |
| RN08 | Falha parcial nunca vira importação validada | Status `CONCLUIDA_COM_PENDENCIAS` ou `FALHOU`, com erros por item |
| RN09 | Recomendação de preço de venda não altera o cadastro | Endpoint de sugestão só leitura; alteração é sempre pelo cadastro de produto |
| RN10 | Variação percentual só com preço anterior maior que zero | Fórmula abaixo, `BigDecimal` com escala 2 e `RoundingMode.HALF_UP` |

```latex
\text{variação \%} = \frac{\text{preço atual} - \text{preço anterior}}{\text{preço anterior}} \times 100
```

**Classificação de um item importado, na ordem:**

1. Preço nulo, zero ou negativo, ou item sem vínculo com produto: PENDENTE.
2. Sem preço válido anterior para a apresentação: NOVO.
3. Preço atual maior que o anterior: AUMENTOU.
4. Preço atual menor que o anterior: DIMINUIU.
5. Iguais: SEM\_ALTERACAO.

**Exemplo esperado na tela de comparação** (do documento do professor):

| Produto | Anterior | Atual | Variação | Classificação | Preço de venda |
| --- | --- | --- | --- | --- | --- |
| Hibisco | R$ 33,00 | R$ 35,00 | +6,06% | Aumentou | R$ 50,00 |
| Alecrim | R$ 15,00 | R$ 14,00 | −6,67% | Diminuiu | R$ 35,00 |
| Camomila | R$ 38,00 | R$ 38,00 | 0,00% | Sem alteração | R$ 65,00 |
| Produto novo | — | R$ 20,00 | — | Novo | — |

## Leitura dos PDFs de referência

Os dois PDFs do professor são tabelas de texto (não escaneadas) com duas colunas de preço por linha, então cada linha gera **duas apresentações comerciais**. A leitura usa Apache PDFBox 3 com `setSortByPosition(true)` e um leitor por layout.

| Layout | Cabeçalho detectado | Colunas | Apresentações por linha | Identificador |
| --- | --- | --- | --- | --- |
| `TABELA_REFERENCIA` (Fornecedor 1) | Ref. Interna, Produto, Unidade, Preço KG, Preço Fardo | referência, descrição, unidade, 2 preços | UNIDADE e FARDO | Referência (ex.: 1076) |
| `ATACADO_VAREJO` (Fornecedor 2) | Descrição, ATACADO, VAREJO | descrição, 2 preços | ATACADO e VAREJO | Descrição normalizada |

Conteúdo atual dos arquivos, igual nos dois fornecedores: ACUCAR DE COCO a 32 e DOCANTE STEVIA COLOR ANDINA 40G a 78 (o texto vem assim no PDF; a descrição original é guardada sem correção).

**Como cada linha é lida**

1. Extrair o texto e quebrar por linha; descartar linhas vazias.
2. Achar a linha de cabeçalho e escolher o leitor (`LeitorTabelaReferencia` ou `LeitorAtacadoVarejo`). Se o cabeçalho não bate com o layout cadastrado no fornecedor, a importação vai para `FALHOU` com motivo `LAYOUT_NAO_RECONHECIDO`.
3. Ler cada linha da direita para a esquerda: os dois últimos tokens são os preços; no layout 1, o primeiro token é a referência numérica e o penúltimo antes dos preços é a unidade (KG, UN, CX, FD).
4. Converter preço no formato brasileiro (`32`, `32,5`, `1.234,56`, `R$ 35,00`) para `BigDecimal`. Token que não converte vira erro do item, não da importação.
5. Normalizar a descrição para comparação: maiúsculas, sem acento, espaços únicos.
6. Quantidade não existe nos dois layouts: o campo fica nulo e a tela mostra "não informada".

Linha que não encaixa no padrão vira item `INVALIDO` com o texto bruto guardado em `linha_original`, para o usuário corrigir na conferência.

## Arquitetura e stack

Um monólito modular em Java e Angular: front Angular no navegador, uma API Spring Boot dividida pelos cinco domínios sugeridos pelo professor e um PostgreSQL. Tudo sobe com um `docker compose up`.


O PDF entra pelo módulo importacao, que lê, valida e associa os itens; precificacao compara com o último preço válido e grava o histórico.

| Camada | Escolha | Por quê |
| --- | --- | --- |
| Back-end | Java 21, Spring Boot 4.1.x, Maven | Referência do professor, build com `mvn verify` |
| Dependências da API | Spring Web, Validation, Data JPA, Flyway, Actuator, PostgreSQL Driver, Apache PDFBox 3, springdoc-openapi | Migrações versionadas, health check para o Compose, Swagger para o front |
| Testes da API | JUnit 5, AssertJ, Mockito, MockMvc, Testcontainers (PostgreSQL) | Regras críticas em teste de unidade, persistência em banco real no CI |
| Front-end | Angular (versão estável atual do CLI), TypeScript, componentes standalone, Angular Router, Reactive Forms, HttpClient, Angular Material | Build com `npm run build`, formulários com as mesmas validações da API |
| Testes do front | Runner padrão gerado pelo Angular CLI, angular-eslint, Prettier | Rodam com `npm test` e `npm run lint` no mesmo job do CI |
| Banco | PostgreSQL 16 | Referência do professor |
| Entrega | Docker multi-stage, Nginx servindo o build do Angular e repassando `/api` | Uma porta só para a demonstração |

**Organização do back-end.** Cada domínio tem quatro pacotes: `dominio` (entidades e regras puras, sem Spring), `aplicacao` (casos de uso), `infraestrutura` (JPA, PDFBox) e `api` (controllers e DTOs). A validação de itens importados usa o Notification Pattern: o item acumula todos os erros numa `Notificacao` em vez de parar no primeiro, e a tela mostra a lista completa.

```
lastro/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/java/br/edu/uniesp/lastro/
│       │   ├── produto/{dominio,aplicacao,infraestrutura,api}
│       │   ├── fornecedor/...
│       │   ├── estoque/...
│       │   ├── importacao/...
│       │   ├── precificacao/...
│       │   └── compartilhado/{notificacao,erro,log}
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── logback-spring.xml
│       │   └── db/migration/V1__...sql
│       └── test/java/br/edu/uniesp/lastro/...
├── frontend/
│   ├── angular.json
│   ├── package.json
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/app/{paginas,componentes,servicos,modelos}
├── docs/
│   ├── PRD.md
│   └── pdfs/ (arquivos de referência e versões para a demo)
├── .github/
│   ├── workflows/ci.yml
│   ├── pull_request_template.md
│   └── ISSUE_TEMPLATE/historia.yml
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## Modelo de dados

Nove tabelas, criadas por migrações Flyway. Histórico de preço é só inserção: nenhuma rotina faz `UPDATE` ou `DELETE` nele. Valores monetários em `NUMERIC(12,2)`, ids em `BIGINT` gerado, exceto importação, que usa `UUID` para aparecer nos logs.

| Tabela | Campos principais | Restrições |
| --- | --- | --- |
| `produto` | codigo, nome, categoria, unidade, preco\_compra, preco\_venda, estoque\_atual, estoque\_minimo, status (ATIVO, INATIVO), criado\_em, atualizado\_em | codigo único; nome e preco\_venda obrigatórios; preços e estoques ≥ 0 |
| `fornecedor` | nome, cnpj, contato, layout\_pdf (TABELA\_REFERENCIA, ATACADO\_VAREJO), ativo | nome obrigatório; cnpj único quando informado |
| `apresentacao_fornecedor` | fornecedor\_id, referencia, descricao\_original, descricao\_normalizada, unidade, modalidade\_preco | único em (fornecedor\_id, chave\_item, unidade, modalidade\_preco), onde chave\_item = referencia ou descricao\_normalizada |
| `vinculo_produto` | apresentacao\_id, produto\_id, origem (AUTOMATICA, MANUAL), confirmado\_em | uma apresentação tem no máximo um vínculo ativo |
| `importacao` | id (UUID), fornecedor\_id, nome\_arquivo, hash\_sha256, tamanho\_bytes, recebida\_em, concluida\_em, status, total\_itens, itens\_validos, itens\_pendentes, motivo\_falha | único em (fornecedor\_id, hash\_sha256) para status diferente de DESCARTADA |
| `item_importado` | importacao\_id, numero\_linha, linha\_original, referencia, descricao, unidade, modalidade\_preco, quantidade, preco, situacao (VALIDO, PENDENTE\_ASSOCIACAO, INVALIDO), erros (JSONB), apresentacao\_id | apresentacao\_id nulo enquanto não associado |
| `historico_preco` | apresentacao\_id, importacao\_id, preco, registrado\_em | preco > 0; só inserção |
| `comparacao_preco` | importacao\_id, item\_importado\_id, preco\_anterior, preco\_atual, diferenca, variacao\_percentual, classificacao | classificacao em (AUMENTOU, DIMINUIU, SEM\_ALTERACAO, NOVO, PENDENTE) |
| `ausencia_item` | apresentacao\_id, importacao\_id, ultimo\_preco\_valido | registrada quando a apresentação existia e não veio no PDF novo |

**Status da importação:** RECEBIDA → EM\_CONFERENCIA → CONCLUIDA ou CONCLUIDA\_COM\_PENDENCIAS. Em qualquer ponto pode ir para FALHOU (erro de leitura) ou DESCARTADA (usuário cancelou). Só CONCLUIDA e CONCLUIDA\_COM\_PENDENCIAS gravam histórico, e só dos itens VALIDO.

## API REST

Todas as rotas ficam sob `/api`, documentadas no Swagger (`/api/swagger-ui.html`). Erros seguem `ProblemDetail` do Spring com um campo `erros` listando cada validação que falhou.

| Método | Rota | Faz | RF |
| --- | --- | --- | --- |
| POST | `/api/produtos` | Cadastra produto | RF01 |
| GET | `/api/produtos?nome=&categoria=&status=&page=` | Lista com filtro e paginação | RF01 |
| GET / PUT | `/api/produtos/{id}` | Consulta e edita | RF01 |
| POST / GET | `/api/fornecedores` | Cadastra e lista | RF02 |
| GET / PUT | `/api/fornecedores/{id}` | Consulta e edita | RF02 |
| POST | `/api/importacoes` (multipart: fornecedorId, arquivo) | Recebe o PDF, calcula hash, extrai e devolve a pré-visualização; 409 se duplicado | RF04, RF05 |
| GET | `/api/importacoes?fornecedorId=&status=` | Lista importações | RF09 |
| GET | `/api/importacoes/{id}` | Detalhe com itens, erros e pendências | RF05, RF06 |
| PUT | `/api/importacoes/{id}/itens/{itemId}` | Corrige descrição, unidade ou preço de um item | RF06 |
| POST | `/api/importacoes/{id}/itens/{itemId}/vinculo` | Associa o item a um produto e salva o vínculo | RF03 |
| POST | `/api/importacoes/{id}/concluir` | Grava histórico dos itens válidos e gera a comparação | RF07, RF08 |
| POST | `/api/importacoes/{id}/descartar` | Cancela sem gravar histórico | RN07 |
| GET | `/api/importacoes/{id}/comparacao` | Resultado classificado com anterior, atual, diferença e percentual | RF08, RF10 |
| GET | `/api/historico?produtoId=&fornecedorId=&importacaoId=` | Histórico de preços filtrado | RF09 |
| GET | `/api/vinculos?fornecedorId=` | Vínculos salvos | RF03 |

Códigos usados: 201 na criação, 400 em validação, 404 quando não existe, 409 em duplicidade, 422 quando o PDF não tem layout reconhecido.

## Telas do front-end (Angular)

Oito telas cobrem o fluxo inteiro da demonstração: cadastrar, importar, conferir, concluir, comparar e consultar.

| Rota | Tela | O que mostra e faz |
| --- | --- | --- |
| `/produtos` | Lista de produtos | Busca por nome e categoria, status, estoque atual e mínimo, botão Novo |
| `/produtos/novo`, `/produtos/:id` | Formulário de produto | Reactive Form com nome e preço de venda obrigatórios e preços não negativos |
| `/fornecedores` | Fornecedores | Lista e formulário, incluindo o layout de PDF do fornecedor |
| `/importacoes` | Histórico de importações | Data, fornecedor, arquivo, total de itens, status |
| `/importacoes/nova` | Envio de PDF | Seleciona fornecedor, escolhe o arquivo, mostra aviso de duplicidade |
| `/importacoes/:id/conferencia` | Conferência | Tabela dos itens extraídos com erros por linha, edição inline, busca de produto para associar, botões Concluir e Descartar |
| `/importacoes/:id/comparacao` | Comparação | Produto, anterior, atual, diferença, variação, classificação e preço de venda; filtro por classificação |
| `/historico` | Histórico de preços | Filtros por produto, fornecedor e importação |

**Acessibilidade da classificação (exigência do professor).** A cor nunca vem sozinha: cada linha tem rótulo em texto, seta ou ícone e o valor com sinal.

| Classificação | Rótulo | Ícone | Variação exibida |
| --- | --- | --- | --- |
| AUMENTOU | Aumentou | seta para cima | +6,06% |
| DIMINUIU | Diminuiu | seta para baixo | −6,67% |
| SEM\_ALTERACAO | Sem alteração | sinal de igual | 0,00% |
| NOVO | Novo | estrela | — |
| PENDENTE | Pendente | alerta | motivo da pendência |

O ícone recebe `aria-hidden="true"` e o rótulo fica visível; valores em reais usam o pipe `currency` com `pt-BR` e BRL.

## Product backlog

São 18 histórias do MVP, 6 histórias técnicas de DevOps e 3 de evolução; cada linha vira uma issue no formato do card US-01 que vocês já usam (descrição, critérios em checklist, prioridade, épico, estimativa). Estimativa em pontos Fibonacci, a ajustar na planning.

**Histórias do MVP**

| ID | Título | Como / quero / para | Critérios de aceitação | Prioridade | Épico | Marco | Pontos |
| --- | --- | --- | --- | --- | --- | --- | --- |
| US-01 | Cadastrar produtos da loja | Como administrador, quero cadastrar produtos com nome, categoria, preço de compra e preço de venda, para manter o catálogo organizado | Permite cadastrar; nome e preço de venda obrigatórios; preço de venda não negativo; produto aparece na consulta após salvar | Alta | Produtos | E1 | 3 |
| US-02 | Consultar produtos | Como administrador, quero buscar produtos por nome, categoria e status, para encontrar um item rapidamente | Busca parcial sem diferenciar maiúsculas; paginação de 20 itens; lista vazia mostra mensagem | Alta | Produtos | E1 | 2 |
| US-03 | Editar produto e estoque | Como administrador, quero editar dados, estoque atual e estoque mínimo, para manter o cadastro correto | Mesmas validações do cadastro; estoques não negativos; código continua único | Alta | Produtos | E1 | 2 |
| US-04 | Cadastrar fornecedor | Como administrador, quero cadastrar fornecedor com o layout do PDF que ele envia, para importar as tabelas dele | Nome obrigatório; layout obrigatório; CNPJ único quando informado | Alta | Fornecedores | E1 | 2 |
| US-05 | Consultar fornecedores | Como administrador, quero listar e editar fornecedores, para manter os dados atualizados | Lista com nome, layout e status; edição preserva importações anteriores | Média | Fornecedores | E1 | 1 |
| US-06 | Enviar PDF do fornecedor | Como administrador, quero enviar o PDF escolhendo o fornecedor, para iniciar uma importação | Aceita só PDF até 5 MB; registra data, arquivo e status RECEBIDA; mostra o id da importação | Alta | Importação | E2 | 3 |
| US-07 | Bloquear reimportação duplicada | Como administrador, quero ser avisado quando enviar o mesmo arquivo de novo, para não duplicar histórico | Mesmo hash e fornecedor retorna 409; tela mostra a importação original; nada é gravado | Alta | Importação | E3 | 2 |
| US-08 | Ler PDF do layout Fornecedor 1 | Como administrador, quero que o sistema leia referência, produto, unidade e os dois preços, para não digitar a tabela | PDF de referência gera 4 itens (2 linhas × 2 modalidades); preços em BigDecimal; linha fora do padrão vira INVALIDO | Alta | Importação | E2 | 5 |
| US-09 | Ler PDF do layout Fornecedor 2 | Como administrador, quero que o sistema leia descrição, atacado e varejo, para importar fornecedores sem referência | PDF de referência gera 4 itens; identificador é a descrição normalizada; cabeçalho errado gera FALHOU | Alta | Importação | E2 | 3 |
| US-10 | Conferir itens extraídos | Como administrador, quero ver e corrigir os itens antes de concluir, para não gravar preço errado | Tabela com todos os itens e erros; edição revalida o item; contadores de válidos e pendentes atualizam | Alta | Importação | E2 | 5 |
| US-11 | Sinalizar itens inválidos | Como administrador, quero ver por que um item ficou pendente, para corrigir | Preço zero ou ausente vira INVALIDO; todos os erros do item aparecem juntos; item inválido não entra no histórico | Alta | Importação | E2 | 3 |
| US-12 | Associar automaticamente quando seguro | Como administrador, quero que itens já conhecidos sejam associados sozinhos, para ganhar tempo | Usa vínculo salvo ou correspondência exata; descrição parecida não associa; origem AUTOMATICA registrada | Alta | Associação | E2 | 3 |
| US-13 | Associar manualmente e lembrar | Como administrador, quero associar um item a um produto e que o sistema lembre disso, para não repetir o trabalho | Busca de produto na conferência; vínculo salvo com origem MANUAL; próxima importação usa o vínculo | Alta | Associação | E2 | 5 |
| US-14 | Concluir importação e gravar histórico | Como administrador, quero concluir a importação, para guardar os preços sem apagar os antigos | Só itens VALIDO entram em historico\_preco; status CONCLUIDA ou CONCLUIDA\_COM\_PENDENCIAS; nenhum registro antigo é alterado | Alta | Histórico | E2 | 3 |
| US-15 | Comparar com o último preço válido | Como administrador, quero comparar cada preço com o último válido da mesma apresentação, para ver o que mudou | Usa o último válido mesmo que não esteja na importação anterior; percentual com 2 casas; sem percentual quando anterior é zero ou não existe | Alta | Precificação | E3 | 5 |
| US-16 | Ver resultado classificado | Como administrador, quero ver aumentou, diminuiu, sem alteração, novo e pendente com diferença e percentual, para decidir a compra | Cinco classificações; rótulo, ícone e valor além da cor; filtro por classificação | Alta | Precificação | E3 | 3 |
| US-17 | Consultar histórico de preços | Como administrador, quero consultar preços por produto, fornecedor e importação, para ver valores antigos | Três filtros combináveis; ordem da mais recente; link para a importação de origem | Média | Histórico | E3 | 3 |
| US-18 | Registrar item ausente | Como administrador, quero saber quando um item não veio no PDF novo, sem que ele seja dado como descontinuado | Grava ausencia\_item; último preço válido continua disponível; aparece na comparação como ausente | Média | Histórico | E3 | 2 |

**Histórias técnicas (DevOps)**

| ID | Título | Critérios de aceitação | Marco | RNF |
| --- | --- | --- | --- | --- |
| TS-01 | Criar repositório e Git flow | Branches main e develop; proteção das duas; templates de PR e issue; .gitignore e .editorconfig | E1 | RNF01 |
| TS-02 | Estrutura inicial back e front | Spring Boot sobe com health em `/actuator/health`; Angular sobe com rota inicial; arquivos de exemplo dos geradores removidos | E1 | RNF02 |
| TS-03 | Pipeline de CI no GitHub Actions | Roda em push e PR; build e testes do back e do front; falha bloqueia o merge | E3 | RNF02, RNF04 |
| TS-04 | Testes das regras críticas | Aumento, redução, igualdade, novo, preço zero e associação pendente cobertos e passando no CI | E3 | RNF03 |
| TS-05 | Dockerfiles e Compose | `docker compose up --build` sobe banco, API e front; configuração por `.env`; nenhum segredo versionado | E4 | RNF05, RNF06 |
| TS-06 | Logs de importação e README | Início, conclusão e falha com id e motivo; README com execução, testes, pipeline e roteiro da demo | E5 | RNF07, RNF08 |

**Evolução (depois do MVP)**

| ID | Título | Critério principal | Prioridade |
| --- | --- | --- | --- |
| US-19 | Sinalizar reposição | Produto com estoque atual ≤ mínimo aparece marcado na lista e num filtro próprio | Baixa |
| US-20 | Sugerir revisão de preço de venda | Calcula preço sugerido pela margem configurada e só mostra; cadastro não muda | Baixa |
| US-21 | Gráfico de evolução de preço | Linha por apresentação ao longo das importações | Baixa |

**Divisão entre Marcelo e Thiago**

A divisão é por fatia vertical: cada um entrega back-end, front-end e testes das próprias histórias, e quem não é dono do card revisa o PR. Assim os dois têm commits em todo o código e PRs revisados pelo outro, que é a evidência pedida no RNF01.

| Responsável | Histórias | Pontos | Histórias técnicas | Revisa PRs de |
| --- | --- | --- | --- | --- |
| Marcelo | US-01, US-02, US-03 (produtos); US-06, US-08, US-09 (envio e leitura do PDF); US-15, US-16 (comparação) | 26 | TS-01 Git flow, TS-03 GitHub Actions, TS-04 testes críticos | Thiago |
| Thiago | US-04, US-05 (fornecedores); US-07 (duplicidade); US-10, US-11 (conferência); US-12, US-13 (associação); US-14, US-17, US-18 (histórico) | 29 | TS-02 estrutura inicial, TS-05 Docker, TS-06 logs e README | Marcelo |

Dependências entre os dois, para combinar a ordem: US-08 e US-09 (Marcelo) entregam os itens que US-10 a US-13 (Thiago) conferem; US-14 (Thiago) grava o histórico que US-15 (Marcelo) compara. O contrato entre essas partes (DTO do item extraído e interface do repositório de histórico) entra num PR pequeno logo no início do E2, revisado pelos dois.

## Git flow, commits e Pull Requests

Ninguém commita direto em `main` ou `develop`: todo código entra por Pull Request revisado pelo outro integrante e com o CI verde.


Cada feature sai de develop e volta por PR revisado pelo outro e com o CI verde; a main só recebe release ou hotfix, sempre com tag, e cada tag dispara a publicação das imagens.

| Branch | Sai de | Volta para | Nome |
| --- | --- | --- | --- |
| `main` | — | — | versão entregue; uma tag por marco (`v0.1.0` no E1, `v0.2.0` no E2...) |
| `develop` | `main` | — | integração do que está pronto |
| feature | `develop` | `develop` | `feature/US-08-leitor-fornecedor-1` |
| técnica | `develop` | `develop` | `chore/TS-03-pipeline-ci` |
| correção | `develop` | `develop` | `fix/US-15-percentual-preco-zero` |
| release | `develop` | `main` e `develop` | `release/v0.3.0` |
| hotfix | `main` | `main` e `develop` | `hotfix/v0.3.1` |

**Ciclo de uma história**

1. Mover o card para Em andamento e criar a branch a partir de `develop` atualizada.
2. Commits pequenos, um assunto por commit.
3. Abrir o PR para `develop` cedo, como rascunho se ainda faltar algo, com `Closes #<número da issue>`.
4. CI precisa passar; o card vai para Em revisão.
5. O outro integrante revisa: comentários por linha e "Request changes" quando houver ajuste.
6. O autor responde cada comentário e corrige em novos commits na mesma branch (sem force push durante a revisão, para o histórico ficar visível).
7. Aprovado e com conversas resolvidas: squash merge, branch apagada, card vai para Concluído sozinho pelo `Closes`.

**Fim de marco:** criar `release/v0.N.0` de `develop`, atualizar README e versão no `pom.xml` e `package.json`, PR para `main` com merge commit, tag `v0.N.0` e merge de volta em `develop`.

**Proteção de branch** (Settings → Rules) para `main` e `develop`: exigir PR, 1 aprovação, descartar aprovação quando entrar commit novo, exigir os checks `backend` e `frontend` do CI, exigir conversas resolvidas, bloquear force push e exclusão.

**Mensagens de commit:** Conventional Commits em português, verbo no presente, minúsculas, até 72 caracteres, sem emoji.

| Tipo | Uso | Exemplo |
| --- | --- | --- |
| feat | funcionalidade | `feat(produto): valida preco de venda nao negativo` |
| fix | correção | `fix(precificacao): ignora preco anterior zerado no percentual` |
| test | testes | `test(precificacao): cobre classificacao sem alteracao` |
| refactor | sem mudar comportamento | `refactor(importacao): extrai conversor de preco` |
| build / ci | Maven, npm, Docker, Actions | `ci: executa testes do front no pull request` |
| docs | README e docs | `docs: adiciona roteiro da demonstracao` |

**Template de PR** (`.github/pull_request_template.md`):

```markdown
## O que muda

## Como testar

## Card
Closes #

## Checklist
- [ ] Testes novos ou ajustados passando
- [ ] Regras de negócio da história cobertas
- [ ] Sem comentários, código morto ou console.log
- [ ] Sem segredo ou senha no código
- [ ] Migração nova em vez de editar migração antiga
```

**O que o revisor olha:** a história atende todos os critérios do card; regra crítica tem teste; nomes explicam o que o código faz; tratamento de erro devolve mensagem útil; nada de `System.out` ou `console.log`; nenhum arquivo gerado pelos templates sobrando. Prazo combinado para revisão: 24 horas.

## GitHub Projects

Cada história do backlog vira uma issue com o formulário abaixo, entra no projeto "Lastro" e é quebrada em sub-issues de tarefa; o card só fecha pelo PR que o resolve.

**Campos do projeto**

| Campo | Tipo | Valores |
| --- | --- | --- |
| Status | seleção | Backlog, Pronto, Em andamento, Em revisão, Concluído |
| Responsável | Assignees | Marcelo ou Thiago (um por card) |
| Prioridade | seleção | Alta, Média, Baixa |
| Épico | seleção | Produtos, Fornecedores, Importação, Associação, Precificação, Histórico, DevOps |
| Marco | Milestone | E0 a E5 |
| Pontos | número | 1, 2, 3, 5, 8 |
| Tipo | Labels | `historia`, `tecnica`, `bug`, `mvp`, `evolucao` |

**Visões**

1. Quadro: colunas por Status, filtro pelo marco atual.
2. Por responsável: quadro agrupado por Assignee, para conferir se a carga está equilibrada.
3. Por marco: tabela agrupada por Milestone com soma de Pontos.

**Automações nativas** (Projects → Workflows): item adicionado vai para Backlog; issue fechada ou PR com merge vai para Concluído; auto-add de issues do repositório com a label `historia` ou `tecnica`.

**Regras do quadro:** no máximo 2 cards por pessoa em Em andamento; card sem critério de aceitação não sai do Backlog; bug achado em revisão vira comentário no PR, bug achado depois do merge vira issue `bug`.

**Como escrever cada card.** Título `US-08 · Ler PDF do layout Fornecedor 1`; descrição no formato como/quero/para; critérios como checklist; tarefas como sub-issues com título de ação. Exemplo de quebra:

| Card | Responsável | Sub-issues (tarefas) |
| --- | --- | --- |
| TS-01 Criar repositório e Git flow | Marcelo | Criar repositório `lastro` e adicionar Thiago como colaborador; criar `develop`; configurar proteção de `main` e `develop`; adicionar template de PR; adicionar formulário de issue `historia.yml`; criar labels e milestones E0 a E5 |
| TS-02 Estrutura inicial back e front | Thiago | Gerar back-end no start.spring.io na pasta `backend`; adicionar PDFBox, springdoc e Spotless no `pom.xml`; gerar Angular na pasta `frontend` com `--skip-git`; adicionar Angular Material e angular-eslint; criar `proxy.conf.json`; criar `.gitignore`, `.editorconfig`, `.env.example` e README inicial na raiz; criar `docker-compose.yml` só com o banco; remover arquivos de exemplo dos geradores; conferir `mvn verify` e `npm run build` |
| US-01 Cadastrar produtos | Marcelo | Migração `V1__cria_produto.sql`; entidade e validações de produto; endpoint POST e GET por id; formulário Angular com validação; testes de unidade das validações; teste de integração do endpoint |
| US-08 Ler PDF Fornecedor 1 | Marcelo | Extrair linhas com PDFBox; detectar cabeçalho do layout; conversor de preço brasileiro; gerar duas apresentações por linha; marcar linha fora do padrão como INVALIDO; testes com o PDF de referência |
| US-15 Comparar preços | Marcelo | Consulta do último preço válido; serviço de classificação; cálculo de diferença e percentual; testes dos cinco casos |

TS-01 vem antes de tudo e TS-02 depende dele: no campo de dependência da issue, marcar TS-02 como "blocked by" TS-01. Os dois fecham no marco E1.

**Como montar o quadro no GitHub**

1. No perfil ou organização: Projects → New project → modelo Board, nome "Lastro".
2. Em Settings do projeto, criar os campos Prioridade, Épico e Pontos; ajustar as opções de Status para Backlog, Pronto, Em andamento, Em revisão, Concluído.
3. Em Settings → Manage access, dar acesso de escrita ao Thiago.
4. Vincular o repositório `lastro` ao projeto (aba Projects do repositório → Link a project).
5. Ativar em Workflows: Item added to project, Item closed, Pull request merged e Auto-add to project com o filtro `is:issue label:historia,tecnica`.
6. Criar as três visões: Quadro, Por responsável (Group by Assignees) e Por marco (Table, Group by Milestone, soma de Pontos).
7. Abrir as issues pelo formulário, começando por TS-01 e TS-02, e preencher Responsável, Prioridade, Épico, Marco e Pontos de cada uma.

Labels e milestones podem ser criados pelo terminal do VSCode com o GitHub CLI, dentro da pasta do repositório:

```
gh label create historia --color 1D76DB
gh label create tecnica --color 5319E7
gh label create bug --color D73A4A
gh label create mvp --color 0E8A16
gh label create evolucao --color FBCA04
gh api repos/{owner}/{repo}/milestones -f title="E0 - Backlog"
gh api repos/{owner}/{repo}/milestones -f title="E1 - Git e estrutura"
gh api repos/{owner}/{repo}/milestones -f title="E2 - Nucleo funcional"
gh api repos/{owner}/{repo}/milestones -f title="E3 - Historico e CI"
gh api repos/{owner}/{repo}/milestones -f title="E4 - Containers"
gh api repos/{owner}/{repo}/milestones -f title="E5 - Operacao e apresentacao"
```

**Formulário da issue** (`.github/ISSUE_TEMPLATE/historia.yml`), no mesmo formato do card US-01:

```yaml
name: História de usuário
description: Nova história do backlog
title: "US-XX · "
labels: [historia, mvp]
body:
  - type: textarea
    id: descricao
    attributes:
      label: Descrição
      placeholder: Como <perfil>, quero <ação>, para <benefício>.
    validations:
      required: true
  - type: textarea
    id: criterios
    attributes:
      label: Critérios de aceitação
      value: |
        - [ ] 
    validations:
      required: true
  - type: dropdown
    id: prioridade
    attributes:
      label: Prioridade
      options: [Alta, Média, Baixa]
  - type: dropdown
    id: epico
    attributes:
      label: Épico
      options: [Produtos, Fornecedores, Importação, Associação, Precificação, Histórico, DevOps]
  - type: input
    id: estimativa
    attributes:
      label: Estimativa
      placeholder: A definir
```

## GitHub Actions

Dois workflows: `ci.yml` roda em todo push e PR e bloqueia o merge quando falha; `release.yml` roda quando uma tag `v*` é criada e publica as imagens no GitHub Container Registry, que é o procedimento de implantação descrito no README.

| Job | Quando | Passos | Falha quando |
| --- | --- | --- | --- |
| `backend` | push e PR | Java 21, cache Maven, `mvn -B verify` (unidade + Testcontainers), publica relatórios | algum teste ou build quebra |
| `frontend` | push e PR | Node 22, `npm ci`, `npm run lint`, `npm run test:ci`, `npm run build` | lint, teste ou build quebra |
| `docker` | depois dos dois | `docker compose build` com `.env.example` | alguma imagem não constrói |
| `publicar` | tag `v*` | build e push das imagens `lastro-api` e `lastro-web` no GHCR | login ou push falha |

No `package.json` do Angular, `test:ci` = `ng test --watch=false` (com `--browsers=ChromeHeadless` se o projeto vier com Karma). Os nomes dos jobs `backend` e `frontend` são os checks exigidos na proteção de branch.

**`.github/workflows/ci.yml`**

```yaml
name: ci

on:
  push:
    branches: [main, develop, "feature/**", "fix/**", "chore/**", "release/**", "hotfix/**"]
  pull_request:
    branches: [main, develop]

jobs:
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: "21"
          cache: maven
      - run: mvn -B verify
      - if: always()
        uses: actions/upload-artifact@v4
        with:
          name: relatorios-backend
          path: backend/target/surefire-reports

  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend
    steps:
      - uses: actions/checkout@v5
      - uses: actions/setup-node@v5
        with:
          node-version: "22"
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
      - run: npm run lint
      - run: npm run test:ci
      - run: npm run build

  docker:
    runs-on: ubuntu-latest
    needs: [backend, frontend]
    steps:
      - uses: actions/checkout@v5
      - run: cp .env.example .env
      - run: docker compose build
```

**`.github/workflows/release.yml`**

```yaml
name: release

on:
  push:
    tags: ["v*"]

permissions:
  contents: read
  packages: write

jobs:
  publicar:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
      - uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      - uses: docker/build-push-action@v6
        with:
          context: backend
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/lastro-api:${{ github.ref_name }}
      - uses: docker/build-push-action@v6
        with:
          context: frontend
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/lastro-web:${{ github.ref_name }}
```

O nome do dono no GHCR precisa estar em minúsculas; se o usuário ou a organização do repositório tiver maiúsculas, troque `github.repository_owner` pelo nome escrito à mão.

## Docker, Compose e variáveis de ambiente

A demonstração sobe com dois comandos, `cp .env.example .env` e `docker compose up --build`, e abre em `http://localhost:8080`. O `.env` fica no `.gitignore`; só o `.env.example` é versionado, sem senha real.

| Serviço | Imagem | Porta | Depende de |
| --- | --- | --- | --- |
| `db` | `postgres:16-alpine` com volume `dados-db` | interna 5432 | — |
| `api` | build de `backend/` (Maven → `eclipse-temurin:21-jre`) | interna 8081 | `db` saudável |
| `web` | build de `frontend/` (Node → `nginx:alpine`) | `${WEB_PORTA}` → 80 | `api` saudável |

**`.env.example`**

```
POSTGRES_DB=lastro
POSTGRES_USER=lastro
POSTGRES_PASSWORD=troque-esta-senha
API_PORTA=8081
WEB_PORTA=8080
UPLOAD_TAMANHO_MAXIMO=5MB
LOG_NIVEL=INFO
```

**`docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - dados-db:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 5s
      retries: 10

  api:
    build: ./backend
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${POSTGRES_DB}
      SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER}
      SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD}
      SERVER_PORT: ${API_PORTA}
      SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE: ${UPLOAD_TAMANHO_MAXIMO}
      LOGGING_LEVEL_BR_EDU_UNIESP_LASTRO: ${LOG_NIVEL}
    depends_on:
      db:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost:${API_PORTA}/actuator/health || exit 1"]
      interval: 10s
      retries: 12

  web:
    build: ./frontend
    ports:
      - "${WEB_PORTA}:80"
    depends_on:
      api:
        condition: service_healthy

volumes:
  dados-db:
```

**`backend/Dockerfile`**

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**`frontend/Dockerfile`**

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/lastro-web/browser /usr/share/nginx/html
```

O `nginx.conf` serve o Angular com `try_files $uri /index.html` e repassa `/api/` para `http://api:8081`. Os testes ficam fora do build da imagem porque já rodam no job `backend` do CI. A imagem `eclipse-temurin:21-jre-alpine` traz `wget` via BusyBox, usado no health check.

## Logs e rastreio de importações

Toda importação deixa rastro em dois lugares: no log da API, com o id da importação em cada linha, e na própria tabela `importacao`, com status e motivo da falha. O id é colocado no MDC do SLF4J (`importacaoId`) no início do processamento e removido no fim.

| Evento | Nível | Campos |
| --- | --- | --- |
| `IMPORTACAO_INICIADA` | INFO | importacaoId, fornecedorId, arquivo, tamanhoBytes, hash |
| `IMPORTACAO_DUPLICADA` | WARN | importacaoId original, fornecedorId, hash |
| `EXTRACAO_CONCLUIDA` | INFO | importacaoId, totalItens, validos, pendentes, invalidos, duracaoMs |
| `IMPORTACAO_CONCLUIDA` | INFO | importacaoId, status final, precosGravados, aumentos, reducoes, novos |
| `IMPORTACAO_FALHOU` | ERROR | importacaoId, motivo (LAYOUT\_NAO\_RECONHECIDO, PDF\_ILEGIVEL, ERRO\_INTERNO), mensagem |
| `IMPORTACAO_DESCARTADA` | INFO | importacaoId |

Padrão da linha no `logback-spring.xml`:

```
%d{yyyy-MM-dd HH:mm:ss} %-5level [importacao=%X{importacaoId:-}] %logger{20} - %msg%n
```

Como mostrar na demonstração:

```
docker compose logs api | grep "importacao=<id>"
docker compose logs -f api
```

O README traz esses dois comandos e um exemplo de saída de uma importação que falhou, para cumprir o RNF07.

## Testes e massa de dados da demonstração

Os seis casos críticos do RNF03 viram testes de unidade do serviço de classificação, sem Spring e sem banco, e rodam no job `backend` do CI. Os nomes descrevem o comportamento, então o relatório do Actions já serve de evidência.

| Teste | Anterior | Atual | Esperado |
| --- | --- | --- | --- |
| `deveClassificarComoAumentouQuandoPrecoSobe` | 33,00 | 35,00 | AUMENTOU, diferença 2,00, +6,06% |
| `deveClassificarComoDiminuiuQuandoPrecoCai` | 15,00 | 14,00 | DIMINUIU, −1,00, −6,67% |
| `deveClassificarSemAlteracaoQuandoPrecoIgual` | 38,00 | 38,00 | SEM\_ALTERACAO, 0,00% |
| `deveClassificarComoNovoSemPrecoAnterior` | — | 20,00 | NOVO, sem percentual |
| `deveDeixarPendenteQuandoPrecoZero` | 32,00 | 0,00 | PENDENTE, não grava histórico |
| `deveDeixarPendenteQuandoItemSemVinculo` | — | 20,00 | PENDENTE até associação manual |

Outros testes que entram junto: usar o último preço válido mesmo quando a importação anterior não trouxe o item; recusar reimportação com o mesmo hash; leitor de cada layout com os PDFs de referência em `src/test/resources/pdfs`; conversão de preço brasileiro; integração dos endpoints com Testcontainers. No Angular: validação do formulário de produto e rótulo de classificação na tabela de comparação.

**PDFs para a demonstração.** Os arquivos do professor são a importação 1. A importação 2 é um `Fornecedor_1_v2.pdf` criado por vocês no mesmo layout (planilha com as mesmas colunas exportada em PDF), pensado para mostrar os cinco resultados exigidos:

| Ref. | Produto | Unidade | Preço KG | Preço Fardo | Resultado na demo |
| --- | --- | --- | --- | --- | --- |
| 1076 | ACUCAR DE COCO | KG | 35 | 32 | KG aumentou (32 → 35); fardo sem alteração |
| 1077 | DOCANTE STEVIA COLOR ANDINA 40G | UN | 74 | 0 | unidade diminuiu (78 → 74); fardo pendente por preço zero |
| 1078 | CHA DE HIBISCO 100G | UN | 20 | 18 | pendente até associar a um produto na conferência, depois novo |

Com esses dois arquivos a demonstração cobre aumento, redução, sem alteração, novo e pendência, e reenviar o mesmo `Fornecedor_1_v2.pdf` mostra o bloqueio de duplicidade. Um terceiro arquivo sem a linha 1077 demonstra o registro de item ausente.

## Padrões de código e de escrita

O código não tem comentários: o que ele faz tem que estar nos nomes de classes, métodos e variáveis. Commits, PRs e README seguem um único estilo, escrito pela dupla.

**Código**

- Nenhum comentário em Java, TypeScript, HTML ou SCSS: nem `//`, nem `/* */`, nem Javadoc, nem `<!-- -->`, nem TODO.
- Métodos curtos com nome de ação (`classificar`, `buscarUltimoPrecoValido`, `converterPreco`); regra de negócio no `dominio`, nunca no controller.
- Domínio em português sem acento (`Importacao`, `precoVenda`); termos de framework ficam como são (`Controller`, `Repository`, `Service`).
- Nada de `System.out`, `console.log`, import sem uso ou código comentado.
- Formatação automática: Spotless no Maven (`mvn spotless:apply`) e Prettier no Angular, com `.editorconfig` na raiz. O CI roda `mvn spotless:check` e `npm run lint`.
- Remover o que os geradores criam e o projeto não usa: `HELP.md` do Spring Initializr, conteúdo de boas-vindas do `app.component.html`, README padrão do Angular, assets de exemplo.

**Verificação antes de abrir o PR** (pode virar um passo do `ci.yml`):

```
git grep -nE '^\s*(//|/\*|\*)' -- '*.java' '*.ts' '*.scss'
git grep -n '<!--' -- '*.html'
git grep -nE 'TODO|FIXME|System\.out|console\.log' -- backend/src frontend/src
```

Os três comandos precisam voltar vazios.

**Commits, PRs e README**

- Cada commit tem um único autor, quem escreveu o código, e nenhuma linha extra de coautoria (`Co-Authored-By`) ou assinatura de ferramenta.
- Histórico incremental: vários commits pequenos ao longo dos dias, não um commit gigante por história; mensagens no padrão da seção de Git flow, nunca "ajustes", "update" ou "final".
- Descrição de PR curta e direta, preenchendo o template; sem emoji, sem títulos extras além dos do template.
- Comentários de revisão objetivos e ligados à linha: o que está errado e o que fazer.
- README sem emoji e sem texto de marketing: requisitos, como rodar, como testar, pipeline, como implantar e roteiro da demonstração.
- Configurações de ferramentas pessoais ficam fora do repositório: `.vscode/`, `.idea/` e pastas locais de editores e assistentes entram no `.gitignore`.

## Marcos e ordem de produção no VSCode

Seis marcos, cada um fechado com uma release e uma tag; as datas ficam como o professor definir no ambiente da disciplina.

| Marco | Entrega | Marcelo | Thiago | Tag |
| --- | --- | --- | --- | --- |
| E0 | Backlog e quadro | Issues US-01 a US-03, US-06, US-08, US-09, US-15, US-16, TS-01, TS-03, TS-04 | Issues US-04, US-05, US-07, US-10 a US-14, US-17, US-18, TS-02, TS-05, TS-06 | — |
| E1 | Git, estrutura e cadastros | TS-01, US-01, US-02, US-03, CI mínimo (build) | TS-02, US-04, US-05 | v0.1.0 |
| E2 | Núcleo funcional | US-06, US-08, US-09 | US-10 a US-14 | v0.2.0 |
| E3 | Histórico, testes e CI | US-15, US-16, TS-03 completo, TS-04 | US-07, US-17, US-18 | v0.3.0 |
| E4 | Containers | `release.yml`, configuração por variável de ambiente na API | TS-05 Dockerfiles e Compose | v0.4.0 |
| E5 | Operação e apresentação | Roteiro da demo e PDFs de teste | TS-06 logs e README | v1.0.0 |

**Primeiros passos no VSCode**

Um repositório só, `lastro`, com back-end e front-end em pastas separadas. O `ci.yml` roda cada job na sua pasta e o Compose faz o build de `./backend` e `./frontend`.

1. Um de vocês cria o repositório `lastro` no GitHub, adiciona o outro como colaborador e aplica as proteções de branch depois de criar a `develop`.
2. Criar o projeto no GitHub Projects com os campos, as visões, os milestones E0 a E5 e as labels; cadastrar as issues pelos formulários.
3. Instalar no VSCode: Extension Pack for Java, Spring Boot Extension Pack, Angular Language Service, ESLint, Prettier, Docker (Container Tools) e GitHub Pull Requests.
4. Clonar, criar a `develop` e a branch da estrutura inicial (comandos abaixo).
5. Gerar o back-end no start.spring.io com a configuração da tabela, descompactar, renomear a pasta `lastro` do zip para `backend` e apagar o `HELP.md`.
6. Gerar o front-end na raiz do repositório com o comando abaixo, depois `ng add @angular/material` e `ng add angular-eslint` dentro de `frontend`; criar `proxy.conf.json` apontando `/api` para `http://localhost:8081`.
7. Na raiz, criar `.gitignore`, `.editorconfig`, `.env.example`, `README.md` e um `docker-compose.yml` inicial só com o banco. Os `.gitignore` que os geradores criaram dentro de cada pasta ficam.
8. Conferir que só existe a pasta `.git` da raiz (nenhuma dentro de `backend/` ou `frontend/`), rodar `mvn verify` e `npm run build`, e abrir o PR `chore/TS-02-estrutura-inicial` para `develop`, revisado pelo outro. Daqui em diante, cada card segue o ciclo da seção de Git flow.

```
lastro/
├── backend/        projeto Spring Boot (zip do start.spring.io)
├── frontend/       projeto Angular
├── docs/
├── .github/
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

**Comandos do passo 4**

```
git clone https://github.com/<usuario>/lastro.git
cd lastro
git checkout -b develop
git push -u origin develop
git checkout -b chore/TS-02-estrutura-inicial
```

**Spring Initializr (start.spring.io)**

| Campo | Valor |
| --- | --- |
| Project | Maven |
| Language | Java |
| Spring Boot | última 4.1.x estável (sem SNAPSHOT, M ou RC) |
| Group | `br.edu.uniesp` |
| Artifact / Name | `lastro` |
| Description | Monitoramento de preços de fornecedores a partir de PDFs |
| Package name | `br.edu.uniesp.lastro` |
| Packaging | Jar |
| Configuration | YAML |
| Java | 21 (o mesmo do `ci.yml` e dos Dockerfiles; trocar nos três lugares se mudar) |

Dependências: Spring Web, Validation, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Spring Boot Actuator, Testcontainers e, se quiserem, Spring Boot DevTools. Não marcar Docker Compose Support: ele tenta subir o Compose ao iniciar a API, e o nosso Compose já inclui a API.

Dependências que o Initializr não oferece, para adicionar no `pom.xml`:

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.7</version>
</dependency>
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>3.1.0</version>
</dependency>
```

O springdoc 3.x é a linha compatível com Spring Boot 4; usar a 3.1.x mais nova do Maven Central. Em `<build><plugins>`, adicionar o `spotless-maven-plugin` na última versão do Maven Central, com o goal `check` na fase `verify`.

**Angular (passo 6)**, rodado na raiz do repositório:

```
npx @angular/cli new lastro-web --directory frontend --routing --style=scss --ssr=false --skip-git
```

O `--skip-git` evita que o Angular crie um repositório git dentro do `lastro`.

**Desenvolvimento local:** `docker compose up db` para o banco, API pelo Spring Boot Dashboard do VSCode e front com `npm start`. O Compose completo fica para testar a imagem antes de cada release.

**Pontos em aberto:** datas dos marcos e peso de cada entrega na nota (professor); se a apresentação final precisa de deploy fora da máquina local.
