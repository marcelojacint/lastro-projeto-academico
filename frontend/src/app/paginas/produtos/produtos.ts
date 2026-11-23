import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-produtos-pagina',
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <h1>Produtos</h1>
    <a mat-flat-button routerLink="/produtos/novo">
      <mat-icon aria-hidden="true">add</mat-icon>
      Novo produto
    </a>
  `,
})
export class ProdutosPagina {}
