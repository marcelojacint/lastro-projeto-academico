import { Component, computed, input } from '@angular/core';
import { CLASSIFICACOES, Classificacao } from '../../modelos/comparacao';
import { EtiquetaStatusComponente } from '../etiqueta-status/etiqueta-status';

@Component({
  selector: 'app-rotulo-classificacao',
  imports: [EtiquetaStatusComponente],
  template: `<app-etiqueta-status [apresentacao]="apresentacao()" />`,
})
export class RotuloClassificacaoComponente {
  readonly classificacao = input.required<Classificacao>();
  protected readonly apresentacao = computed(() => CLASSIFICACOES[this.classificacao()]);
}
