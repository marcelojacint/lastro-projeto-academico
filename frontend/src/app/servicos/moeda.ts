import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const FORMATO_MOEDA =
  /^(?<sinal>-)?\s*(?:R\$)?\s*(?<sinalDepoisDoSimbolo>-)?\s*(?<inteiro>\d{1,3}(?:\.\d{3})+|\d+)(?:,(?<centavos>\d{1,2}))?$/;

const formatador = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function converterMoeda(texto: string | null | undefined): number | null {
  const limpo = (texto ?? '').trim();
  if (limpo === '') {
    return null;
  }
  const partes = FORMATO_MOEDA.exec(limpo)?.groups;
  if (!partes) {
    return Number.NaN;
  }
  const inteiro = partes['inteiro'].replace(/\./g, '');
  const centavos = partes['centavos'] ?? '0';
  const valor = Number(`${inteiro}.${centavos}`);
  const negativo = partes['sinal'] !== undefined || partes['sinalDepoisDoSimbolo'] !== undefined;
  return negativo ? -valor : valor;
}

export function formatarMoeda(valor: number | null | undefined): string {
  if (valor === null || valor === undefined) {
    return '';
  }
  return formatador.format(valor).replace(/\s/g, ' ');
}

export function validarMoeda(): ValidatorFn {
  return (controle: AbstractControl<string | null>): ValidationErrors | null => {
    const valor = converterMoeda(controle.value);
    if (valor === null) {
      return null;
    }
    if (Number.isNaN(valor)) {
      return { moeda: true };
    }
    if (valor < 0) {
      return { negativo: true };
    }
    return null;
  };
}
