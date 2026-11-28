import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Classificacao } from '../../modelos/comparacao';
import { RotuloClassificacaoComponente } from './rotulo-classificacao';

describe('RotuloClassificacaoComponente', () => {
  let fixture: ComponentFixture<RotuloClassificacaoComponente>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RotuloClassificacaoComponente],
    }).compileComponents();
    fixture = TestBed.createComponent(RotuloClassificacaoComponente);
  });

  async function mostrar(classificacao: Classificacao): Promise<HTMLElement> {
    fixture.componentRef.setInput('classificacao', classificacao);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it.each<[Classificacao, string, string, string]>([
    ['AUMENTOU', 'Aumentou', 'arrow_upward', 'tom-erro'],
    ['DIMINUIU', 'Diminuiu', 'arrow_downward', 'tom-queda'],
    ['SEM_ALTERACAO', 'Sem alteração', 'equal', 'tom-neutro'],
    ['NOVO', 'Novo', 'star', 'tom-andamento'],
    ['PENDENTE', 'Pendente', 'warning', 'tom-pendente'],
  ])(
    'deve mostrar %s com rotulo em texto, icone decorativo e cor',
    async (classificacao, rotulo, icone, tom) => {
      const elemento = await mostrar(classificacao);

      const etiqueta = elemento.querySelector('.status');
      const simbolo = elemento.querySelector('mat-icon');
      expect(etiqueta?.textContent?.replace(icone, '').trim()).toBe(rotulo);
      expect(simbolo?.textContent?.trim()).toBe(icone);
      expect(simbolo?.getAttribute('aria-hidden')).toBe('true');
      expect(etiqueta?.classList).toContain(tom);
    },
  );
});
