import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MensagemErro } from '../../modelos/item-importado';

@Component({
  selector: 'app-erros-item',
  imports: [MatIconModule],
  templateUrl: './erros-item.html',
  styleUrl: './erros-item.scss',
})
export class ErrosItemComponente {
  readonly erros = input<MensagemErro[]>([]);
}
