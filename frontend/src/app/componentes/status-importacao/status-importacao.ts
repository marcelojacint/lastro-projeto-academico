import { Component, computed, input } from '@angular/core';
import { STATUS_IMPORTACAO, StatusImportacao } from '../../modelos/importacao';
import { EtiquetaStatusComponente } from '../etiqueta-status/etiqueta-status';

@Component({
  selector: 'app-status-importacao',
  imports: [EtiquetaStatusComponente],
  template: `<app-etiqueta-status [apresentacao]="apresentacao()" />`,
})
export class StatusImportacaoComponente {
  readonly status = input.required<StatusImportacao>();
  protected readonly apresentacao = computed(() => STATUS_IMPORTACAO[this.status()]);
}
