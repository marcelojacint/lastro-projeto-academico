# Lastro

Sistema web que transforma as tabelas de preço em PDF dos fornecedores em histórico rastreável e compara cada novo preço com o último preço válido conhecido.

Projeto Integrador da disciplina de DevOps.

## Problema

Uma loja de produtos naturais recebe tabelas de preço em PDF de vários fornecedores. A conferência manual não mostra aumentos, reduções, valores antigos nem quais itens correspondem a quais produtos da loja.

O Lastro importa o PDF, permite conferir os itens extraídos, associa cada item a um produto da loja, grava o histórico sem sobrescrever nada e classifica cada preço como aumentou, diminuiu, sem alteração, novo ou pendente.

## Backlog

- Quadro: [GitHub Projects · Lastro](https://github.com/users/marcelojacint/projects/1)
- Cards: [issues do repositório](https://github.com/marcelojacint/lastro-projeto-academico/issues)
- Detalhamento, decisões e definições de pronto: [docs/backlog.md](docs/backlog.md)

Cada card corresponde a um Pull Request. Histórias que usam a mesma tela foram unidas num card só e mantêm os dois códigos no título.

| Marco | Entrega | Tag |
| --- | --- | --- |
| E0 | Backlog e quadro | — |
| E1 | Git, estrutura e cadastros | v0.1.0 |
| E2 | Núcleo funcional: envio, leitura, conferência e associação | v0.2.0 |
| E3 | Comparação, histórico, testes e CI | v0.3.0 |
| E4 | Containers | v0.4.0 |
| E5 | Operação e apresentação | v1.0.0 |

## Histórias de usuário

Perfil único: administrador da loja.

| Código | História | Como, quero, para | Épico | Marco | Pontos |
| --- | --- | --- | --- | --- | --- |
| US-01 e US-03 | [Cadastrar e editar produtos](https://github.com/marcelojacint/lastro-projeto-academico/issues/4) | Como administrador, quero cadastrar produtos com nome, categoria, preços e estoques e depois editar esses dados, para manter o catálogo organizado e correto. | Produtos | E1 | 5 |
| US-02 | [Consultar produtos](https://github.com/marcelojacint/lastro-projeto-academico/issues/6) | Como administrador, quero buscar produtos por nome, categoria e status, para encontrar um item rapidamente. | Produtos | E1 | 2 |
| US-04 e US-05 | [Cadastrar e consultar fornecedores](https://github.com/marcelojacint/lastro-projeto-academico/issues/5) | Como administrador, quero cadastrar, listar e editar fornecedores com o layout do PDF que cada um envia, para importar as tabelas deles e manter os dados atualizados. | Fornecedores | E1 | 3 |
| US-06 | [Enviar PDF do fornecedor](https://github.com/marcelojacint/lastro-projeto-academico/issues/8) | Como administrador, quero enviar o PDF escolhendo o fornecedor, para iniciar uma importação. | Importação | E2 | 5 |
| US-07 | [Bloquear reimportação duplicada](https://github.com/marcelojacint/lastro-projeto-academico/issues/16) | Como administrador, quero ser avisado quando enviar o mesmo arquivo de novo, para não duplicar histórico. | Importação | E3 | 2 |
| US-08 | [Ler PDF do layout Fornecedor 1](https://github.com/marcelojacint/lastro-projeto-academico/issues/11) | Como administrador, quero que o sistema leia referência, produto, unidade e os dois preços, para não digitar a tabela. | Importação | E2 | 5 |
| US-09 | [Ler PDF do layout Fornecedor 2](https://github.com/marcelojacint/lastro-projeto-academico/issues/12) | Como administrador, quero que o sistema leia descrição, atacado e varejo, para importar fornecedores sem referência. | Importação | E2 | 3 |
| US-10 | [Conferir itens extraídos](https://github.com/marcelojacint/lastro-projeto-academico/issues/13) | Como administrador, quero ver e corrigir os itens antes de concluir, para não gravar preço errado. | Importação | E2 | 5 |
| US-11 | [Sinalizar itens inválidos](https://github.com/marcelojacint/lastro-projeto-academico/issues/9) | Como administrador, quero ver por que um item ficou pendente, para corrigir. | Importação | E2 | 3 |
| US-12 | [Associar automaticamente quando seguro](https://github.com/marcelojacint/lastro-projeto-academico/issues/10) | Como administrador, quero que itens já conhecidos sejam associados sozinhos, para ganhar tempo. | Associação | E2 | 3 |
| US-13 | [Associar manualmente e lembrar](https://github.com/marcelojacint/lastro-projeto-academico/issues/14) | Como administrador, quero associar um item a um produto e que o sistema lembre disso, para não repetir o trabalho. | Associação | E2 | 5 |
| US-14 | [Concluir importação e gravar histórico](https://github.com/marcelojacint/lastro-projeto-academico/issues/15) | Como administrador, quero concluir ou descartar a importação, para guardar os preços sem apagar os antigos. | Histórico | E2 | 5 |
| US-15 e US-16 | [Comparar com o último preço válido e ver o resultado classificado](https://github.com/marcelojacint/lastro-projeto-academico/issues/17) | Como administrador, quero comparar cada preço com o último válido da mesma apresentação e ver aumentou, diminuiu, sem alteração, novo e pendente com diferença e percentual, para decidir a compra. | Precificação | E3 | 8 |
| US-17 | [Consultar histórico de preços](https://github.com/marcelojacint/lastro-projeto-academico/issues/18) | Como administrador, quero consultar preços por produto, fornecedor e importação, para ver valores antigos. | Histórico | E3 | 3 |
| US-18 | [Registrar item ausente](https://github.com/marcelojacint/lastro-projeto-academico/issues/19) | Como administrador, quero saber quando um item não veio no PDF novo, sem que ele seja dado como descontinuado. | Histórico | E3 | 2 |

Total do MVP: 59 pontos em histórias de usuário, mais 2 pontos do contrato técnico TS-07.

## Histórias técnicas

| Código | Card | Objetivo | Marco |
| --- | --- | --- | --- |
| TS-01 | [Criar repositório e Git flow](https://github.com/marcelojacint/lastro-projeto-academico/issues/1) | Branches, proteção, templates, labels, marcos e quadro | E1 |
| TS-02 | [Estrutura inicial back e front](https://github.com/marcelojacint/lastro-projeto-academico/issues/2) | Spring Boot, Angular e banco local prontos para as histórias | E1 |
| TS-03 | [CI mínimo no GitHub Actions](https://github.com/marcelojacint/lastro-projeto-academico/issues/3) | Build e testes em todo push e PR, bloqueando o merge quando falham | E1 |
| TS-07 | [Contrato entre importação, conferência e histórico](https://github.com/marcelojacint/lastro-projeto-academico/issues/7) | Esquema e interfaces compartilhadas para o trabalho em paralelo no E2 | E2 |
| TS-04 | [CI completo e testes das regras críticas](https://github.com/marcelojacint/lastro-projeto-academico/issues/20) | Casos críticos cobertos, verificação de padrões e build das imagens | E3 |
| TS-05 | [Dockerfiles e Compose](https://github.com/marcelojacint/lastro-projeto-academico/issues/21) | Sistema completo com um `docker compose up --build` | E4 |
| TS-06 | [Logs de importação e README](https://github.com/marcelojacint/lastro-projeto-academico/issues/22) | Importações rastreáveis no log e roteiro da demonstração | E5 |

## Evolução depois do MVP

| Código | História | Como, quero, para | Pontos |
| --- | --- | --- | --- |
| US-19 | [Sinalizar reposição](https://github.com/marcelojacint/lastro-projeto-academico/issues/23) | Como administrador, quero ver quais produtos estão com estoque atual menor ou igual ao mínimo, para repor a tempo. | 2 |
| US-20 | [Sugerir revisão de preço de venda](https://github.com/marcelojacint/lastro-projeto-academico/issues/24) | Como administrador, quero ver um preço de venda sugerido pela margem configurada, para revisar o preço sem alterar o cadastro. | 3 |
| US-21 | [Gráfico de evolução de preço](https://github.com/marcelojacint/lastro-projeto-academico/issues/25) | Como administrador, quero ver a evolução do preço de cada item ao longo das importações, para identificar tendências. | 5 |

## Como trabalhamos

- Nenhum commit direto em `main` ou `develop`: todo código entra por Pull Request revisado pelo outro integrante e com o CI verde.
- Cards sem responsável fixo: quem puxa o card para Em andamento se atribui a ele.
- Um card só sai do Backlog com critérios de aceitação, tarefas e dependências definidos.
- Commits em Conventional Commits, em português.

As instruções de execução, testes, pipeline e implantação entram neste README junto com os cards TS-02, TS-05 e TS-06.

## Equipe

Marcelo e Thiago.
