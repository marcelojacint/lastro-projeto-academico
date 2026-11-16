import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

interface ItemMenu {
  rota: string;
  rotulo: string;
  icone: string;
}

@Component({
  selector: 'app-root',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly menu: ItemMenu[] = [
    { rota: '/produtos', rotulo: 'Produtos', icone: 'inventory_2' },
    { rota: '/fornecedores', rotulo: 'Fornecedores', icone: 'local_shipping' },
    { rota: '/importacoes', rotulo: 'Importações', icone: 'upload_file' },
    { rota: '/historico', rotulo: 'Histórico', icone: 'history' },
  ];
}
