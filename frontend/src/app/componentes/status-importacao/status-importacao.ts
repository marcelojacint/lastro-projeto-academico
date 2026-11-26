import { Component, computed, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { STATUS_IMPORTACAO, StatusImportacao } from '../../modelos/importacao';

@Component({
  selector: 'app-status-importacao',
  imports: [MatIconModule],
  template: `
    <span class="status" [class]="'tom-' + apresentacao().tom">
      <mat-icon aria-hidden="true">{{ apresentacao().icone }}</mat-icon>
      <span>{{ apresentacao().rotulo }}</span>
    </span>
  `,
  styleUrl: './status-importacao.scss',
})
export class StatusImportacaoComponente {
  readonly status = input.required<StatusImportacao>();
  protected readonly apresentacao = computed(() => STATUS_IMPORTACAO[this.status()]);
}
