import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const DIGITOS_CNPJ = 14;
const CARACTERES_PERMITIDOS = /^[\d./\-\s]+$/;

export function digitosCnpj(texto: string): string | null {
  const digitos = texto.replace(/\D/g, '');
  return texto.trim() === '' ? null : digitos;
}

export function formatarCnpj(cnpj: string | null): string {
  if (cnpj === null || cnpj.length !== DIGITOS_CNPJ) {
    return cnpj ?? '';
  }
  return cnpj.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
}

export function validarCnpj(): ValidatorFn {
  return (controle: AbstractControl<string>): ValidationErrors | null => {
    const texto = controle.value ?? '';
    if (texto.trim() === '') {
      return null;
    }
    const valido = CARACTERES_PERMITIDOS.test(texto) && digitosCnpj(texto)?.length === DIGITOS_CNPJ;
    return valido ? null : { cnpj: true };
  };
}
