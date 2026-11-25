import { FormControl } from '@angular/forms';
import { digitosCnpj, formatarCnpj, validarCnpj } from './cnpj';

describe('cnpj', () => {
  it('deve manter so os digitos e tratar texto vazio como nulo', () => {
    expect(digitosCnpj('12.345.678/0001-90')).toBe('12345678000190');
    expect(digitosCnpj('   ')).toBeNull();
    expect(digitosCnpj('')).toBeNull();
  });

  it('deve formatar cnpj de quatorze digitos', () => {
    expect(formatarCnpj('12345678000190')).toBe('12.345.678/0001-90');
    expect(formatarCnpj(null)).toBe('');
  });

  it('deve aceitar vazio ou quatorze digitos com ou sem mascara', () => {
    const validar = validarCnpj();

    expect(validar(new FormControl(''))).toBeNull();
    expect(validar(new FormControl('12345678000190'))).toBeNull();
    expect(validar(new FormControl('12.345.678/0001-90'))).toBeNull();
  });

  it('deve recusar quantidade errada de digitos ou letras', () => {
    const validar = validarCnpj();

    expect(validar(new FormControl('12.345.678/0001-9'))).toEqual({ cnpj: true });
    expect(validar(new FormControl('12345678000190A'))).toEqual({ cnpj: true });
  });
});
