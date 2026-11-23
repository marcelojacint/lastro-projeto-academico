import { Routes } from '@angular/router';
import { FornecedoresPagina } from './paginas/fornecedores/fornecedores';
import { HistoricoPagina } from './paginas/historico/historico';
import { ImportacoesPagina } from './paginas/importacoes/importacoes';
import { ProdutoFormularioPagina } from './paginas/produto-formulario/produto-formulario';
import { ProdutosPagina } from './paginas/produtos/produtos';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'produtos' },
  { path: 'produtos', component: ProdutosPagina, title: 'Produtos · Lastro' },
  { path: 'produtos/novo', component: ProdutoFormularioPagina, title: 'Novo produto · Lastro' },
  { path: 'produtos/:id', component: ProdutoFormularioPagina, title: 'Editar produto · Lastro' },
  { path: 'fornecedores', component: FornecedoresPagina, title: 'Fornecedores · Lastro' },
  { path: 'importacoes', component: ImportacoesPagina, title: 'Importações · Lastro' },
  { path: 'historico', component: HistoricoPagina, title: 'Histórico · Lastro' },
  { path: '**', redirectTo: 'produtos' },
];
