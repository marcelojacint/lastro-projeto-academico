# Lastro — Backlog detalhado

Detalhamento dos cards do PRD para cadastrar no GitHub Projects. Cada card vira uma issue; as tarefas ficam como checklist dentro da issue; "Bloqueado por" vira dependência "blocked by". Nenhum card entra em Em andamento sem estar completo aqui.

Cada card corresponde a um PR. Histórias que usam a mesma tela e sairiam no mesmo PR foram unidas e mantêm os dois códigos no título (US-01 e US-03, US-04 e US-05, US-15 e US-16), para continuar rastreáveis ao PRD.

## Decisões assumidas neste documento

Pontos que o PRD deixava em aberto ou com conflito. As tarefas abaixo já seguem estas decisões; o que for mudado aqui precisa ser refletido nos cards antes do E0 fechar.

| Código | Decisão | Motivo |
| --- | --- | --- |
| D01 | `develop` é a branch padrão do repositório | `Closes #` só fecha a issue quando o PR entra na branch padrão; templates de PR e issue também são lidos dela |
| D02 | Modalidades: `UNITARIO` e `FARDO` (layout 1), `ATACADO` e `VAREJO` (layout 2) | O PRD alternava entre KG e UNIDADE; a coluna "Preço KG" vale também para itens UN |
| D03 | A apresentação é resolvida na extração (busca ou cria pela chave); o que falta para o item sair de `PENDENTE_ASSOCIACAO` é o vínculo com produto | Separa item do fornecedor (apresentação) de produto da loja (vínculo) |
| D04 | `chave_item` é coluna gerada `COALESCE(referencia, descricao_normalizada)`; restrição `UNIQUE NULLS NOT DISTINCT (fornecedor_id, chave_item, unidade, modalidade_preco)` | O layout 2 não tem unidade e NULL não bloqueia duplicata em `UNIQUE` comum |
| D05 | Duplicidade de hash ignora importações `DESCARTADA` e `FALHOU` (índice único parcial) | Permite reenviar o arquivo depois de corrigir o layout do fornecedor |
| D06 | `vinculo_produto` tem coluna `ativo` e índice único parcial `(apresentacao_id) WHERE ativo` | Garante um vínculo ativo por apresentação sem apagar o histórico de vínculos |
| D07 | Correspondência exata (RN05) = descrição normalizada do item igual ao nome normalizado de exatamente um produto ativo | Dois produtos com o mesmo nome não associam sozinhos |
| D08 | Na conclusão, a comparação é calculada antes de inserir o histórico da importação, na mesma transação | Evita comparar o item com o próprio preço |
| D09 | Item ausente aparece numa seção "Itens ausentes" da tela de comparação, fora das cinco classificações | A classificação continua com cinco valores |
| D10 | Sem autenticação no MVP | "Administrador fixo" sem login; Spring Security complicaria todos os testes de integração |
| D11 | Migrações Flyway versionadas por data e hora: `V2026_11_20_1430__cria_produto.sql`; `spring.flyway.out-of-order=true` só no perfil local | Dois integrantes criando `V2` em branches paralelas geram conflito |
| D12 | Banco publicado na máquina em `${DB_PORTA}` (padrão 5433); dentro do Compose a API usa `db:5432` | A porta 5432 da máquina de desenvolvimento já está ocupada por outro projeto |
| D13 | Porta interna da API fixa em 8081; `API_PORTA` sai do `.env.example` | O `nginx.conf` repassa para `http://api:8081` |
| D14 | Código do produto opcional e único quando informado | Mantém a restrição do modelo de dados do PRD |
| D15 | Novo card TS-07 para o contrato entre importação, conferência e histórico | O PRD prevê esse PR no início do E2 mas não tinha card |
| D16 | Tela `/importacoes` (lista) entra na US-06; endpoint e botão Descartar entram na US-14 | Estavam no PRD sem história responsável |
| D17 | Tarefas como checklist na issue, não como sub-issues | Reduz o quadro de cerca de 150 cards para 25 sem perder o detalhamento; o GitHub mostra o progresso da checklist no card |
| D18 | US-01 e US-03, US-04 e US-05, US-15 e US-16 viram um card cada; TS-03 fica só com o CI mínimo do E1 e o restante do CI entra no TS-04 | Mesma tela ou mesmo PR; cada card fecha dentro de um único marco |
| D19 | Cards sem responsável fixo: quem puxa o card se atribui ao movê-lo para Em andamento; o outro integrante revisa o PR | A dupla escolhe os cards conforme a necessidade de construção |

## Definição de pronto para começar

Um card só sai de Backlog para Pronto Para Começar quando nenhum card que o bloqueia está aberto e ele tem descrição como/quero/para, critérios de aceitação e tarefas em checklist, dependências marcadas, prioridade, épico, marco e pontos. O responsável é definido quando alguém puxa o card para Em andamento.

## Definição de concluído

Vale para todos os cards, além dos critérios de cada um:

- PR para `develop` com `Closes #<issue>`, template preenchido, revisado e aprovado pelo outro integrante.
- CI verde (`backend` e `frontend`).
- Regra de negócio da história coberta por teste.
- Nenhum comentário, `System.out`, `console.log` ou import sem uso; os três `git grep` do PRD voltam vazios.
- Migração nova em vez de editar migração já mesclada.
- Erros da API em `ProblemDetail` com campo `erros`.

## Ordem de execução

Os cards de um mesmo marco que não dependem um do outro podem andar em paralelo, um com cada integrante.

| Marco | Cards em ordem | Podem andar em paralelo |
| --- | --- | --- |
| E0 | Revisão do backlog e montagem do quadro | — |
| E1 | TS-01, TS-02, TS-03, US-01 e 03, US-04 e 05, US-02 | TS-03, US-01 e 03, US-04 e 05 (todos depois do TS-02) |
| E2 | TS-07, US-06, US-11, US-12, US-08, US-09, US-10, US-13, US-14 | US-06 com US-11 e US-12; US-08 e US-09 com US-12 |
| E3 | US-15 e 16, US-07, US-17, US-18, TS-04 | US-07, US-17 e US-18 com US-15 e 16 |
| E4 | TS-05 | — |
| E5 | TS-06 | — |

Pontos do MVP: 61. US-06 e US-14 foram reestimadas de 3 para 5 por causa da D16.

## Mapa de dependências

| Card | Bloqueado por |
| --- | --- |
| TS-01 | — |
| TS-02 | TS-01 |
| TS-03 | TS-02 |
| US-01 e 03, US-04 e 05 | TS-02 |
| US-02 | US-01 e 03 |
| TS-07 | US-01 e 03, US-04 e 05 |
| US-06 | TS-07 |
| US-08 | US-06 |
| US-09 | US-08 |
| US-11 | TS-07 |
| US-12 | TS-07 |
| US-10 | US-08, US-11 |
| US-13 | US-10, US-12 |
| US-14 | US-13 |
| US-07 | US-06 |
| US-15 e 16 | US-14 |
| US-17 | US-14 |
| US-18 | US-14 |
| TS-04 | TS-03, US-15 e 16, US-12 |
| TS-05 | TS-04 |
| TS-06 | TS-05 |

---

## Histórias técnicas

### TS-01 · Criar repositório e Git flow

Marco: E1 · Prioridade: Alta · Épico: DevOps · Labels: tecnica, mvp · Bloqueado por: —

Descrição: Como dupla, queremos o repositório com Git flow, proteção de branch e quadro configurados, para que todo código entre por PR revisado e rastreado em card.

Critérios de aceitação:
- [ ] Branches `main` e `develop` existem; `develop` é a branch padrão (D01)
- [ ] `main` e `develop` protegidas: PR obrigatório, 1 aprovação, aprovação descartada com commit novo, conversas resolvidas, sem force push, sem exclusão
- [ ] Template de PR e formulários de issue (história e técnica) funcionando
- [ ] `.gitignore` e `.editorconfig` na raiz
- [ ] Labels, milestones E0 a E5 e projeto "Lastro" com campos, visões e automações
- [ ] Todas as issues deste backlog cadastradas com tarefas e dependências

Tarefas:
1. Adicionar Thiago como colaborador com permissão de escrita
2. Criar `develop` a partir de `main`, publicar e defini-la como branch padrão
3. Adicionar `.gitignore` da raiz (`.env`, `.vscode/`, `.idea/`, `.claude/`, `target/`, `node_modules/`, `dist/`) e `.editorconfig` (UTF-8, LF, indentação compatível com Spotless e Prettier)
4. Adicionar `.github/pull_request_template.md` com o texto do PRD
5. Adicionar `.github/ISSUE_TEMPLATE/historia.yml` e `tecnica.yml` (este com label `tecnica` e campo de RNF)
6. Adicionar `docs/PRD.md` e `docs/backlog.md`
7. Configurar regras de proteção de `main` e `develop` sem checks obrigatórios (os checks entram na tarefa 2 do TS-03)
8. Criar as 5 labels e os 6 milestones pelo `gh`
9. Criar o projeto "Lastro" com campos Prioridade, Épico, Pontos, Status, as três visões e as automações do PRD
10. Cadastrar as issues deste backlog e marcar as dependências

### TS-02 · Estrutura inicial back e front

Marco: E1 · Prioridade: Alta · Épico: DevOps · Labels: tecnica, mvp · Bloqueado por: TS-01

Descrição: Como dupla, queremos os projetos Spring Boot e Angular gerados e limpos no mesmo repositório, para começar as histórias sobre uma base que compila e testa.

Critérios de aceitação:
- [ ] `mvn verify` passa, incluindo teste de contexto com Testcontainers
- [ ] API local responde `UP` em `/actuator/health` usando o banco do Compose
- [ ] Swagger em `/api/swagger-ui.html`
- [ ] Angular sobe com `npm start`, rota inicial e layout base; `npm run build`, `npm run lint` e `npm run test:ci` passam
- [ ] `docker compose up db` sobe o PostgreSQL 16 em `localhost:${DB_PORTA}` (D12)
- [ ] Só existe o `.git` da raiz; nenhum arquivo de exemplo dos geradores

Tarefas:
1. Gerar o back-end no start.spring.io com a configuração do PRD em `backend/` e apagar `HELP.md`
2. Conferir a dependência `flyway-database-postgresql`; adicionar PDFBox 3.0.x, springdoc 3.1.x e `spotless-maven-plugin` com `check` na fase `verify`
3. Configurar `application.yml`: porta 8081, datasource `localhost:${DB_PORTA:5433}`, caminho do Swagger, health do Actuator, multipart de 5 MB (arquivo e requisição), perfil local com `out-of-order` (D11)
4. Manter um teste de contexto com Testcontainers (PostgreSQL 16 e `@ServiceConnection`) rodando no `mvn verify`
5. Gerar o Angular em `frontend/` com o comando do PRD; `ng add @angular/material`; `ng add angular-eslint`; instalar Prettier; criar o script `test:ci`
6. Criar `proxy.conf.json` com `/api` para `http://localhost:8081` e ligá-lo ao `npm start`
7. Criar layout base com barra e menu para Produtos, Fornecedores, Importações e Histórico; rota inicial redireciona para `/produtos`
8. Criar `.env.example` (D12, D13) e README inicial com o passo a passo de desenvolvimento local
9. Criar `docker-compose.yml` só com o serviço `db` (imagem, volume, healthcheck, porta `${DB_PORTA}:5432`)
10. Remover arquivos de exemplo dos geradores e rodar os três `git grep` do PRD

### TS-03 · CI mínimo no GitHub Actions

Marco: E1 · Prioridade: Alta · Épico: DevOps · Labels: tecnica, mvp · Bloqueado por: TS-02

Descrição: Como dupla, queremos build e testes automáticos em todo push e PR desde o E1, para que nada quebrado chegue em `develop` ou `main`.

Critérios de aceitação:
- [ ] Workflow roda em push e PR nas branches do PRD
- [ ] Jobs `backend` (`mvn -B verify`) e `frontend` (`npm ci`, lint, `test:ci`, build) passam
- [ ] `backend` e `frontend` exigidos na proteção de `main` e `develop`

Tarefas:
1. Criar `.github/workflows/ci.yml` com os jobs `backend` e `frontend` do PRD
2. Depois da primeira execução verde, marcar `backend` e `frontend` como checks obrigatórios em `main` e `develop`

### TS-04 · CI completo e testes das regras críticas

Marco: E3 · Prioridade: Alta · Épico: DevOps · Labels: tecnica, mvp · Bloqueado por: TS-03, US-15 e 16, US-12

Descrição: Como dupla, queremos o pipeline completo e os casos críticos do RNF03 cobertos por testes com nomes descritivos, para que o relatório do CI sirva de evidência.

Critérios de aceitação:
- [ ] Os seis testes do PRD existem com os nomes exatos e passam no CI
- [ ] Teste de último preço válido quando a importação anterior não trouxe o item
- [ ] Teste de que a comparação não usa o preço da própria importação (D08)
- [ ] Testes de duplicidade, dos dois leitores e do conversor de preço existem
- [ ] Testes do front: validação do formulário de produto e rótulo de classificação
- [ ] Relatórios do Surefire publicados como artefato
- [ ] Job `padroes` falha quando encontra comentário, `TODO`, `System.out` ou `console.log`
- [ ] Job `docker` constrói as imagens com `.env.example`
- [ ] PR com teste falhando fica bloqueado para merge

Tarefas:
1. Conferir os seis testes do RNF03 em `ClassificadorPrecoTest` e completar `deveDeixarPendenteQuandoPrecoZero` e `deveDeixarPendenteQuandoItemSemVinculo`
2. Criar teste de integração: importação 1 com o item, importação 2 sem o item, importação 3 compara com a 1
3. Criar teste de integração para a D08
4. Conferir os testes de US-07, US-08, US-09 e do conversor e completar os casos que faltarem
5. Conferir os testes do front de US-01 e US-16
6. Publicar `target/surefire-reports` com `upload-artifact`
7. Criar o job `padroes` com os três `git grep` do PRD e incluí-lo nos checks obrigatórios
8. Criar o job `docker` dependente de `backend` e `frontend`
9. Abrir um PR de demonstração com teste falhando, registrar o bloqueio no card e fechar o PR sem merge
10. Registrar no card o link do relatório do Actions com os testes

### TS-05 · Dockerfiles e Compose

Marco: E4 · Prioridade: Alta · Épico: DevOps · Labels: tecnica, mvp · Bloqueado por: TS-04

Descrição: Como dupla, queremos subir banco, API e front com um comando, para demonstrar o sistema em qualquer máquina com Docker.

Critérios de aceitação:
- [ ] `cp .env.example .env` e `docker compose up --build` sobem `db`, `api` e `web`; sistema abre em `http://localhost:8080`
- [ ] Toda configuração vem do `.env`; nenhum segredo versionado
- [ ] Upload de PDF de até 5 MB passa pelo Nginx
- [ ] Tag `v*` publica `lastro-api` e `lastro-web` no GHCR

Tarefas:
1. Criar `backend/Dockerfile` multi-stage do PRD com runtime `eclipse-temurin:21-jre-alpine`
2. Criar `frontend/Dockerfile` multi-stage e `nginx.conf` com `try_files`, repasse de `/api/` para `http://api:8081` e `client_max_body_size 5m`
3. Criar `.dockerignore` em `backend/` e `frontend/`
4. Adicionar `api` e `web` ao Compose com healthchecks e `depends_on` do PRD
5. Garantir que toda configuração da API venha de variável de ambiente e que o `application.yml` não tenha senha
6. Criar `.github/workflows/release.yml` do PRD
7. Testar do zero: `docker compose down -v`, subir com `--build` e importar o PDF de referência

### TS-06 · Logs de importação e README

Marco: E5 · Prioridade: Alta · Épico: DevOps · Labels: tecnica, mvp · Bloqueado por: TS-05

Descrição: Como dupla, queremos cada importação rastreável no log e um README que permita rodar, testar e demonstrar o sistema.

Critérios de aceitação:
- [ ] Os seis eventos do PRD aparecem com nível e campos definidos
- [ ] Toda linha do processamento de uma importação traz `importacao=<id>`
- [ ] `docker compose logs api | grep "importacao=<id>"` mostra a importação inteira
- [ ] README com requisitos, execução local, Compose, testes, pipeline, implantação e roteiro da demo, sem emoji
- [ ] PDFs da demonstração em `docs/pdfs`

Tarefas:
1. Criar `logback-spring.xml` com o padrão do PRD
2. Colocar `importacaoId` no MDC no início do processamento e removê-lo em `finally`
3. Registrar os eventos `IMPORTACAO_INICIADA`, `IMPORTACAO_DUPLICADA`, `EXTRACAO_CONCLUIDA`, `IMPORTACAO_CONCLUIDA`, `IMPORTACAO_FALHOU` e `IMPORTACAO_DESCARTADA`
4. Testar com `OutputCaptureExtension` que a falha de layout registra `IMPORTACAO_FALHOU` com o motivo
5. Escrever o README final com os comandos de log e um exemplo de saída de importação que falhou
6. Criar `Fornecedor_1_v2.pdf` (tabela do PRD) e `Fornecedor_1_v3.pdf` (sem a linha 1077) em `docs/pdfs`
7. Escrever o roteiro da demonstração no README e ensaiá-lo do zero com o Compose

### TS-07 · Contrato entre importação, conferência e histórico

Marco: E2 · Prioridade: Alta · Épico: Importação · Pontos: 2 · Labels: tecnica, mvp · Bloqueado por: US-01 e 03, US-04 e 05

Descrição: Como dupla, queremos o esquema e as interfaces compartilhadas definidos num PR pequeno, para trabalharmos em paralelo no E2 sem conflito.

Critérios de aceitação:
- [ ] Migrações de `importacao`, `apresentacao_fornecedor`, `item_importado` e `historico_preco` com as restrições do PRD e das decisões D04 e D05
- [ ] Enums `StatusImportacao`, `SituacaoItem`, `ModalidadePreco` (D02), `LayoutPdf`, `MotivoFalha`
- [ ] `ItemExtraido` (record) e interface `LeitorPdf` em `importacao.dominio`; `Notificacao` em `compartilhado`
- [ ] Interface de consulta do último preço válido por apresentação
- [ ] Nenhuma regra implementada além das estruturas; aprovado pelos dois

Tarefas:
1. Migração de `importacao` (UUID, índice único parcial de hash)
2. Migração de `apresentacao_fornecedor` com `chave_item` gerada e restrição única (D04)
3. Migração de `item_importado` com `erros` JSONB
4. Migração de `historico_preco` com `CHECK (preco > 0)`
5. Enums, `ItemExtraido`, `LeitorPdf` e `Notificacao`
6. Entidades JPA e repositórios das quatro tabelas, com a assinatura `buscarUltimoPrecoValido(apresentacaoId, importacaoIgnorada)`
7. Teste de integração que sobe as migrações e confere as restrições únicas

---

## Histórias do MVP

### US-01 e US-03 · Cadastrar e editar produtos

Marco: E1 · Prioridade: Alta · Épico: Produtos · Pontos: 5 · Labels: historia, mvp · Bloqueado por: TS-02

Descrição: Como administrador, quero cadastrar produtos com nome, categoria, preços e estoques e depois editar esses dados, para manter o catálogo organizado e correto.

Critérios de aceitação:
- [ ] Permite cadastrar e editar todos os campos do RF01
- [ ] Nome e preço de venda obrigatórios; preços e estoques não negativos; status padrão ATIVO
- [ ] Código único quando informado (D14): repetido responde 409, no cadastro e na edição
- [ ] Erros de validação respondem 400 com a lista completa em `erros`
- [ ] Produto inexistente responde 404; `atualizado_em` muda a cada edição
- [ ] Produto aparece na consulta após salvar

Tarefas:
1. Migração `cria_produto` com campos e restrições do PRD
2. Entidade `Produto` com as regras no domínio
3. Tratamento global de erro em `compartilhado/erro` com `ProblemDetail` e campo `erros` (400, 404, 409)
4. `POST /api/produtos`, `GET /api/produtos/{id}` e `PUT /api/produtos/{id}`
5. Testes de unidade das validações do produto
6. Teste de integração dos endpoints (201, 200, 400, 404, 409)
7. Front: modelo `Produto` e `ProdutoService`
8. Front: formulário em `/produtos/novo` e `/produtos/:id` com Reactive Form, mensagens por campo e moeda pt-BR
9. Front: interceptor que mostra os erros da API em snackbar
10. Front: teste do formulário (obrigatórios e valor negativo)

### US-02 · Consultar produtos

Marco: E1 · Prioridade: Alta · Épico: Produtos · Pontos: 2 · Labels: historia, mvp · Bloqueado por: US-01 e 03

Descrição: Como administrador, quero buscar produtos por nome, categoria e status, para encontrar um item rapidamente.

Critérios de aceitação:
- [ ] Busca parcial por nome sem diferenciar maiúsculas; filtros combináveis de categoria e status
- [ ] Paginação de 20 itens
- [ ] Lista vazia mostra mensagem
- [ ] Lista mostra status, estoque atual e mínimo, e tem botão Novo

Tarefas:
1. `GET /api/produtos?nome=&categoria=&status=&page=` com paginação de 20
2. Teste de integração dos filtros e da paginação
3. Front: tela `/produtos` com tabela, campo de busca, filtros e paginador
4. Front: mensagem de lista vazia, botão Novo e link de edição em cada linha

### US-04 e US-05 · Cadastrar e consultar fornecedores

Marco: E1 · Prioridade: Alta · Épico: Fornecedores · Pontos: 3 · Labels: historia, mvp · Bloqueado por: TS-02

Descrição: Como administrador, quero cadastrar, listar e editar fornecedores com o layout do PDF que cada um envia, para importar as tabelas deles e manter os dados atualizados.

Critérios de aceitação:
- [ ] Nome e layout obrigatórios no cadastro e na edição
- [ ] CNPJ guardado só com dígitos, 14 dígitos quando informado, único (409); campo vazio vira nulo
- [ ] Fornecedor nasce ativo
- [ ] Lista com nome, layout e status
- [ ] Não existe exclusão: fornecedor é inativado, preservando importações anteriores

Tarefas:
1. Migração `cria_fornecedor` com as restrições do PRD
2. Entidade `Fornecedor` com as regras no domínio
3. `POST /api/fornecedores`, `GET /api/fornecedores`, `GET /api/fornecedores/{id}` e `PUT /api/fornecedores/{id}`
4. Testes de unidade e de integração (201, 200, 400, 404, 409)
5. Front: modelo e serviço de fornecedor
6. Front: tela `/fornecedores` com lista, formulário com seleção do layout e ativação

### US-06 · Enviar PDF do fornecedor

Marco: E2 · Prioridade: Alta · Épico: Importação · Pontos: 5 · Labels: historia, mvp · Bloqueado por: TS-07

Descrição: Como administrador, quero enviar o PDF escolhendo o fornecedor, para iniciar uma importação.

Critérios de aceitação:
- [ ] Aceita só PDF (assinatura `%PDF-`, não só a extensão) até 5 MB; o resto responde 400
- [ ] Fornecedor inexistente responde 404; inativo responde 400
- [ ] Registra nome do arquivo, tamanho, hash SHA-256, data e status RECEBIDA
- [ ] Resposta e tela mostram o id da importação
- [ ] Tela `/importacoes` lista data, fornecedor, arquivo, total de itens e status (D16)

Tarefas:
1. Caso de uso de recebimento: valida, calcula o SHA-256 e grava a importação
2. `POST /api/importacoes` (multipart `fornecedorId`, `arquivo`)
3. `GET /api/importacoes?fornecedorId=&status=` ordenado pela mais recente
4. Testes: tipo inválido, tamanho excedido, fornecedor inexistente e inativo, hash calculado
5. Front: tela `/importacoes/nova` com seleção de fornecedor ativo e validação local do arquivo
6. Front: tela `/importacoes` com link para conferência ou comparação conforme o status

### US-07 · Bloquear reimportação duplicada

Marco: E3 · Prioridade: Alta · Épico: Importação · Pontos: 2 · Labels: historia, mvp · Bloqueado por: US-06

Descrição: Como administrador, quero ser avisado quando enviar o mesmo arquivo de novo, para não duplicar histórico.

Critérios de aceitação:
- [ ] Mesmo hash e fornecedor responde 409 com o id da importação original
- [ ] Nada é gravado na tentativa duplicada
- [ ] Importações DESCARTADA e FALHOU não bloqueiam (D05)
- [ ] Tela mostra o aviso com link para a importação original

Tarefas:
1. Verificar duplicidade antes de gravar e tratar a violação do índice único como 409
2. Testes: duplicado, mesmo arquivo em outro fornecedor, reenvio após descarte e após falha
3. Front: aviso de duplicidade com link na tela de envio

### US-08 · Ler PDF do layout Fornecedor 1

Marco: E2 · Prioridade: Alta · Épico: Importação · Pontos: 5 · Labels: historia, mvp · Bloqueado por: US-06

Descrição: Como administrador, quero que o sistema leia referência, produto, unidade e os dois preços, para não digitar a tabela.

Critérios de aceitação:
- [ ] PDF de referência gera 4 itens (2 linhas, modalidades UNITARIO e FARDO) com preços em `BigDecimal`
- [ ] Preço aceita `32`, `32,5`, `1.234,56` e `R$ 35,00`
- [ ] Linha fora do padrão vira INVALIDO com `linha_original`; preço que não converte é erro do item
- [ ] Cabeçalho diferente do layout do fornecedor gera FALHOU com `LAYOUT_NAO_RECONHECIDO` e resposta 422 com o id
- [ ] PDF ilegível gera FALHOU com `PDF_ILEGIVEL`
- [ ] Importação lida fica EM_CONFERENCIA com contadores preenchidos

Tarefas:
1. Adicionar os PDFs do professor em `docs/pdfs` e `backend/src/test/resources/pdfs`
2. Extrair o texto com PDFBox (`setSortByPosition(true)`) e registrar no PR a saída real dos dois PDFs
3. Conversor de preço brasileiro com testes, incluindo tokens inválidos
4. Normalizador de descrição (maiúsculas, sem acento, espaço único) com testes
5. Detecção de cabeçalho e escolha do leitor pelo layout do fornecedor
6. `LeitorTabelaReferencia`: remove `R$`, lê da direita para a esquerda e gera dois itens por linha
7. Marcar linhas fora do padrão como INVALIDO com o texto bruto
8. Ligar a extração ao envio: grava os itens, resolve as apresentações (D03), atualiza status e contadores
9. Teste com o PDF de referência conferindo os 4 itens e seus valores

### US-09 · Ler PDF do layout Fornecedor 2

Marco: E2 · Prioridade: Alta · Épico: Importação · Pontos: 3 · Labels: historia, mvp · Bloqueado por: US-08

Descrição: Como administrador, quero que o sistema leia descrição, atacado e varejo, para importar fornecedores sem referência.

Critérios de aceitação:
- [ ] PDF de referência gera 4 itens (modalidades ATACADO e VAREJO)
- [ ] Identificador é a descrição normalizada; unidade fica nula
- [ ] Cabeçalho errado gera FALHOU

Tarefas:
1. `LeitorAtacadoVarejo`: descrição é tudo antes dos dois preços
2. Teste com o PDF de referência conferindo os 4 itens
3. Teste de fornecedor com layout 1 recebendo PDF do layout 2 e o inverso

### US-10 · Conferir itens extraídos

Marco: E2 · Prioridade: Alta · Épico: Importação · Pontos: 5 · Labels: historia, mvp · Bloqueado por: US-08, US-11

Descrição: Como administrador, quero ver e corrigir os itens antes de concluir, para não gravar preço errado.

Critérios de aceitação:
- [ ] Tela mostra todos os itens com situação, erros e produto vinculado
- [ ] Quantidade nula aparece como "não informada"
- [ ] Editar descrição, unidade ou preço revalida o item e atualiza os contadores
- [ ] Edição só é aceita com a importação EM_CONFERENCIA (409 nos outros status)

Tarefas:
1. `GET /api/importacoes/{id}` com cabeçalho, contadores e itens
2. `PUT /api/importacoes/{id}/itens/{itemId}`: revalida, resolve a apresentação de novo e recalcula os contadores
3. Testes de integração do detalhe e da edição
4. Front: tela `/importacoes/:id/conferencia` com tabela, contadores e filtro por situação
5. Front: edição do item com validação e atualização da linha

### US-11 · Sinalizar itens inválidos

Marco: E2 · Prioridade: Alta · Épico: Importação · Pontos: 3 · Labels: historia, mvp · Bloqueado por: TS-07

Descrição: Como administrador, quero ver por que um item ficou pendente, para corrigir.

Critérios de aceitação:
- [ ] Preço ausente, zero ou negativo deixa o item INVALIDO
- [ ] Todos os erros do item aparecem juntos (Notification Pattern)
- [ ] Item INVALIDO nunca entra no histórico

Tarefas:
1. Regras de validação do item no domínio acumulando na `Notificacao` (preço ausente, zero ou negativo; descrição vazia; referência ou unidade ausente no layout 1)
2. Mensagens com código e texto legível, gravadas em `erros`
3. Testes de unidade com vários erros no mesmo item
4. Front: coluna de erros com a lista completa, ícone de alerta e texto

### US-12 · Associar automaticamente quando seguro

Marco: E2 · Prioridade: Alta · Épico: Associação · Pontos: 3 · Labels: historia, mvp · Bloqueado por: TS-07

Descrição: Como administrador, quero que itens já conhecidos sejam associados sozinhos, para ganhar tempo.

Critérios de aceitação:
- [ ] Usa o vínculo ativo da apresentação quando existe
- [ ] Sem vínculo, associa só por correspondência exata (D07) e registra origem AUTOMATICA
- [ ] Descrição parecida ou nome repetido em dois produtos não associa
- [ ] Item sem vínculo fica PENDENTE_ASSOCIACAO

Tarefas:
1. Migração `cria_vinculo_produto` com `ativo` e índice único parcial (D06)
2. Regra de associação automática no domínio
3. Aplicar a regra na extração e depois de cada correção de item
4. Testes: vínculo salvo, correspondência exata, descrição parecida, nome repetido

### US-13 · Associar manualmente e lembrar

Marco: E2 · Prioridade: Alta · Épico: Associação · Pontos: 5 · Labels: historia, mvp · Bloqueado por: US-10, US-12

Descrição: Como administrador, quero associar um item a um produto e que o sistema lembre disso, para não repetir o trabalho.

Critérios de aceitação:
- [ ] Busca de produto na própria tela de conferência
- [ ] Vínculo salvo com origem MANUAL; vínculo anterior da apresentação fica inativo
- [ ] Ao associar, a tela oferece o mesmo produto para a outra modalidade da mesma linha
- [ ] Próxima importação do mesmo fornecedor usa o vínculo
- [ ] `GET /api/vinculos?fornecedorId=` lista os vínculos ativos

Tarefas:
1. `POST /api/importacoes/{id}/itens/{itemId}/vinculo`
2. `GET /api/vinculos?fornecedorId=`
3. Testes de integração, incluindo duas importações seguidas
4. Front: busca de produto com autocomplete na linha do item
5. Front: oferta de associar a outra modalidade da mesma linha

### US-14 · Concluir importação e gravar histórico

Marco: E2 · Prioridade: Alta · Épico: Histórico · Pontos: 5 · Labels: historia, mvp · Bloqueado por: US-13

Descrição: Como administrador, quero concluir ou descartar a importação, para guardar os preços sem apagar os antigos.

Critérios de aceitação:
- [ ] Só itens VALIDO entram em `historico_preco`
- [ ] Status final CONCLUIDA, ou CONCLUIDA_COM_PENDENCIAS quando sobra item pendente ou inválido
- [ ] Nenhum registro antigo de histórico é alterado
- [ ] Descartar leva para DESCARTADA sem gravar histórico (D16)
- [ ] Concluir e descartar só aceitam importação EM_CONFERENCIA (409 nos outros status)

Tarefas:
1. `POST /api/importacoes/{id}/concluir` numa transação, com ponto de entrada para a comparação antes da inserção (D08)
2. `POST /api/importacoes/{id}/descartar`
3. Testes: item inválido fora do histórico, histórico anterior intacto, descarte sem histórico, status inválido
4. Front: botões Concluir e Descartar com confirmação; concluir leva para a comparação

### US-15 e US-16 · Comparar com o último preço válido e ver o resultado classificado

Marco: E3 · Prioridade: Alta · Épico: Precificação · Pontos: 8 · Labels: historia, mvp · Bloqueado por: US-14

Descrição: Como administrador, quero comparar cada preço com o último válido da mesma apresentação e ver aumentou, diminuiu, sem alteração, novo e pendente com diferença e percentual, para decidir a compra.

Critérios de aceitação:
- [ ] Usa o último preço válido da apresentação, mesmo que não esteja na importação anterior
- [ ] Classificação segue a ordem do PRD (PENDENTE, NOVO, AUMENTOU, DIMINUIU, SEM_ALTERACAO)
- [ ] Percentual com 2 casas, `HALF_UP`, multiplicando por 100 antes de dividir; nulo quando o anterior não existe ou é zero
- [ ] Comparação gravada em `comparacao_preco` para todos os itens, incluindo pendentes
- [ ] Tabela com produto, anterior, atual, diferença, variação, classificação e preço de venda
- [ ] Cada classificação tem rótulo, ícone com `aria-hidden="true"` e valor com sinal, além da cor
- [ ] Valores em reais com `currency` pt-BR e BRL
- [ ] Filtro por classificação
- [ ] Seção "Itens ausentes" quando houver (D09)

Tarefas:
1. Migração `cria_comparacao_preco`
2. Consulta do último preço válido ignorando a importação atual
3. `ClassificadorPreco` no domínio
4. Cálculo de diferença e variação percentual
5. Integrar a comparação na conclusão, antes de gravar o histórico
6. Testes de unidade dos cinco casos com os valores do PRD (+6,06% e −6,67%)
7. `GET /api/importacoes/{id}/comparacao?classificacao=` com teste de integração
8. Front: componente de rótulo de classificação com teste para as cinco classificações
9. Front: tela `/importacoes/:id/comparacao` com tabela, filtro e seção de itens ausentes

### US-17 · Consultar histórico de preços

Marco: E3 · Prioridade: Média · Épico: Histórico · Pontos: 3 · Labels: historia, mvp · Bloqueado por: US-14

Descrição: Como administrador, quero consultar preços por produto, fornecedor e importação, para ver valores antigos.

Critérios de aceitação:
- [ ] Filtros de produto (pelo vínculo), fornecedor e importação combináveis
- [ ] Ordem da mais recente para a mais antiga, paginado
- [ ] Cada linha tem link para a importação de origem

Tarefas:
1. `GET /api/historico?produtoId=&fornecedorId=&importacaoId=`
2. Teste de integração dos filtros combinados
3. Front: tela `/historico` com filtros e tabela

### US-18 · Registrar item ausente

Marco: E3 · Prioridade: Média · Épico: Histórico · Pontos: 2 · Labels: historia, mvp · Bloqueado por: US-14

Descrição: Como administrador, quero saber quando um item não veio no PDF novo, sem que ele seja dado como descontinuado.

Critérios de aceitação:
- [ ] Na conclusão, toda apresentação do fornecedor com histórico que não veio no PDF gera `ausencia_item` com o último preço válido
- [ ] Nada do item é alterado ou desativado
- [ ] Ausências aparecem na resposta da comparação

Tarefas:
1. Migração `cria_ausencia_item`
2. Registro das ausências na conclusão
3. Incluir as ausências na resposta de `GET /api/importacoes/{id}/comparacao`
4. Teste de integração com o `Fornecedor_1_v3.pdf` (sem a linha 1077)

---

## Evolução (depois do MVP)

Sem marco até o v1.0.0. Labels: historia, evolucao.

### US-19 · Sinalizar reposição

Prioridade: Baixa · Épico: Produtos · Pontos: 2

Critérios de aceitação:
- [ ] Produto com estoque atual menor ou igual ao mínimo aparece marcado na lista, com texto e ícone
- [ ] Filtro `reposicao=true` na consulta

Tarefas:
1. Campo calculado de reposição na resposta de produto
2. Filtro na consulta com teste
3. Front: marcador e filtro na lista

### US-20 · Sugerir revisão de preço de venda

Prioridade: Baixa · Épico: Precificação · Pontos: 3

Critérios de aceitação:
- [ ] Preço sugerido calculado pela margem configurada a partir do último preço de compra
- [ ] Só leitura: o cadastro do produto não muda (RN09)

Tarefas:
1. Margem configurada por variável de ambiente
2. `GET /api/produtos/{id}/sugestao-preco`
3. Teste de que o produto não é alterado
4. Front: coluna de sugestão na comparação

### US-21 · Gráfico de evolução de preço

Prioridade: Baixa · Épico: Histórico · Pontos: 5

Critérios de aceitação:
- [ ] Linha por apresentação ao longo das importações
- [ ] Valores acessíveis também em tabela

Tarefas:
1. Endpoint da série de preços por apresentação
2. Escolha da biblioteca de gráfico registrada no PR
3. Front: gráfico e tabela na tela de histórico
