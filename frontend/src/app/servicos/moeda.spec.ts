import { converterMoeda, formatarMoeda } from './moeda';

describe('moeda', () => {
  it('deve converter valores no formato brasileiro', () => {
    expect(converterMoeda('32')).toBe(32);
    expect(converterMoeda('32,5')).toBe(32.5);
    expect(converterMoeda('1.234,56')).toBe(1234.56);
    expect(converterMoeda('R$ 35,00')).toBe(35);
    expect(converterMoeda('-R$ 1,00')).toBe(-1);
  });

  it('deve tratar texto vazio como valor nao informado', () => {
    expect(converterMoeda('')).toBeNull();
    expect(converterMoeda('   ')).toBeNull();
  });

  it('deve sinalizar texto que nao e moeda', () => {
    expect(converterMoeda('dez')).toBeNaN();
    expect(converterMoeda('12,345')).toBeNaN();
  });

  it('deve formatar em reais', () => {
    expect(formatarMoeda(1234.5)).toBe('R$ 1.234,50');
    expect(formatarMoeda(null)).toBe('');
  });
});
