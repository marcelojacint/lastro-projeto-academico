import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ApresentacaoStatus } from '../../modelos/importacao';

@Component({
  selector: 'app-etiqueta-status',
  imports: [MatIconModule],
  template: `
    <span class="status" [class]="'tom-' + apresentacao().tom">
      <mat-icon aria-hidden="true">{{ apresentacao().icone }}</mat-icon>
      <span>{{ apresentacao().rotulo }}</span>
    </span>
  `,
  styleUrl: './etiqueta-status.scss',
})
export class EtiquetaStatusComponente {
  readonly apresentacao = input.required<ApresentacaoStatus>();
}
